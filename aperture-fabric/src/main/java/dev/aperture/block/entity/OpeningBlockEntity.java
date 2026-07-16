package dev.aperture.block.entity;

import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.serialization.OpeningInstanceNbtCodec;
import dev.aperture.registry.ApertureBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Holds a placed {@link OpeningInstance}.
 * The instance is persisted directly to NBT for world save/load.
 */
public final class OpeningBlockEntity extends BlockEntity {
	private @Nullable OpeningInstance instance;

	public OpeningBlockEntity(BlockPos pos, BlockState state) {
		super(ApertureBlockEntities.OPENING, pos, state);
	}

	public Optional<OpeningInstance> getInstance() {
		return Optional.ofNullable(instance);
	}

	public void setInstance(OpeningInstance instance) {
		this.instance = instance;
		setChanged();

		// Also register in runtime instance store
		try {
			ApertureRuntime.get().instances().put(instance);
		} catch (IllegalStateException notInitialized) {
			// Runtime not initialized yet (e.g., during world load)
			// Instance will be registered when runtime initializes
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (instance != null) {
			output.storeBoolean("hasInstance", true);
			OpeningInstanceNbtCodec.write(output, instance);
		} else {
			output.storeBoolean("hasInstance", false);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		boolean hasInstance = input.readBoolean("hasInstance").orElse(false);
		if (hasInstance) {
			try {
				instance = OpeningInstanceNbtCodec.read(input);

				// Register in runtime instance store
				try {
					ApertureRuntime.get().instances().put(instance);
				} catch (IllegalStateException notInitialized) {
					// Will be registered later
				}
			} catch (Exception e) {
				// Log error but don't crash world load
				System.err.println("Failed to load OpeningInstance at " + getBlockPos() + ": " + e.getMessage());
				instance = null;
			}
		}
	}
}

