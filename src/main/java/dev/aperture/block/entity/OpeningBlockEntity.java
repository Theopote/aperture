package dev.aperture.block.entity;

import dev.aperture.api.ApertureApi;
import dev.aperture.core.instance.OpeningInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the world link to a placed {@link OpeningInstance}.
 */
public final class OpeningBlockEntity extends BlockEntity {
	private @Nullable UUID instanceId;

	public OpeningBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public Optional<UUID> instanceId() {
		return Optional.ofNullable(instanceId);
	}

	public void setInstanceId(UUID instanceId) {
		this.instanceId = instanceId;
		setChanged();
	}

	public Optional<OpeningInstance> resolveInstance() {
		if (instanceId == null) {
			return Optional.empty();
		}

		try {
			return ApertureApi.get().instances().findById(instanceId);
		} catch (IllegalStateException notInitialized) {
			return Optional.empty();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (instanceId != null) {
			output.store("instance_id", UUIDUtil.CODEC, instanceId);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		instanceId = input.read("instance_id", UUIDUtil.CODEC).orElse(null);
	}
}
