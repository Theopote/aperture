package dev.aperture.block.entity;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.fabric.serialization.ArchitecturalObjectSnapshotNbtCodec;
import dev.aperture.fabric.serialization.OpeningInstanceNbtCodec;
import dev.aperture.fabric.runtime.FabricRuntimeLifecycle;
import dev.aperture.registry.ApertureBlockEntities;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Minecraft anchor for a placed opening. The live RuntimeObjectSession remains authoritative.
 */
public final class OpeningBlockEntity extends BlockEntity {
	private @Nullable OpeningInstance instance;
	private @Nullable ArchitecturalObjectSnapshot runtimeSnapshot;

	public OpeningBlockEntity(BlockPos pos, BlockState state) {
		super(ApertureBlockEntities.OPENING, pos, state);
	}

	/**
	 * Gets the current opening instance.
	 */
	public @Nullable OpeningInstance getInstance() {
		return instance;
	}

	/**
	 * Returns the current opening instance wrapped in an Optional.
	 */
	public Optional<OpeningInstance> resolveInstance() {
		return Optional.ofNullable(instance);
	}

	/**
	 * Sets the opening instance and marks dirty.
	 */
	public void setInstance(@Nullable OpeningInstance instance) {
		this.instance = instance;
		setChanged();
	}

	/**
	 * Returns the durable runtime snapshot when this placement uses the runtime
	 * object model.
	 */
	public Optional<ArchitecturalObjectSnapshot> resolveRuntimeSnapshot() {
		if (runtimeSnapshot == null) return Optional.empty();
		return FabricRuntimeLifecycle.snapshot(runtimeSnapshot.instance().objectId())
			.or(() -> Optional.of(runtimeSnapshot));
	}

	/**
	 * Replaces the durable runtime snapshot and marks the block entity dirty.
	 */
	public void setRuntimeSnapshot(@Nullable ArchitecturalObjectSnapshot runtimeSnapshot) {
		this.runtimeSnapshot = runtimeSnapshot;
		activateRuntimeIfServer();
		setChanged();
	}

	/** Captures the authoritative session before a chunk unload and then deactivates it. */
	public void unloadRuntimeSession() {
		if (level == null || level.isClientSide()) return;
		resolveRuntimeSnapshot().ifPresent(snapshot -> {
			runtimeSnapshot = snapshot;
			FabricRuntimeLifecycle.unload(snapshot.instance().objectId());
		});
	}

	/** Permanently removes the runtime object; unlike unload it does not retain a snapshot. */
	public void removeRuntimeObject() {
		if (level == null || level.isClientSide()) return;
		if (runtimeSnapshot != null) {
			FabricRuntimeLifecycle.remove(runtimeSnapshot.instance().objectId());
			runtimeSnapshot = null;
			setChanged();
		}
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		activateRuntimeIfServer();
	}

	private void activateRuntimeIfServer() {
		if (runtimeSnapshot != null && level != null && !level.isClientSide()
			&& FabricRuntimeLifecycle.installed()) {
			FabricRuntimeLifecycle.activate(runtimeSnapshot);
		}
	}

	@Override
	public void setRemoved() {
		unloadRuntimeSession();
		super.setRemoved();
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		activateRuntimeIfServer();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		removeRuntimeObject();
		super.preRemoveSideEffects(pos, state);
	}

	@Override
	protected void saveAdditional(@NotNull ValueOutput output) {
		super.saveAdditional(output);
		if (instance != null) {
			output.putBoolean("hasInstance", true);
			OpeningInstanceNbtCodec.write(output, instance);
		} else {
			output.putBoolean("hasInstance", false);
		}
		resolveRuntimeSnapshot().ifPresent(snapshot ->
			ArchitecturalObjectSnapshotNbtCodec.write(output, snapshot));
	}

	@Override
	protected void loadAdditional(@NotNull ValueInput input) {
		super.loadAdditional(input);
		boolean hasInstance = input.getBooleanOr("hasInstance", false);
		if (hasInstance) {
			instance = OpeningInstanceNbtCodec.read(input);
		} else {
			instance = null;
		}
		runtimeSnapshot = ArchitecturalObjectSnapshotNbtCodec.read(input).orElse(null);
		activateRuntimeIfServer();
	}
}
