package dev.aperture.block.entity;

import dev.aperture.core.serialization.OpeningInstanceNbtCodec;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.registry.ApertureBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Holds a placed {@link OpeningInstance} with full NBT persistence.
 */
public final class OpeningBlockEntity extends BlockEntity {
	private @Nullable OpeningInstance instance;

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
	 * Sets the opening instance and marks dirty.
	 */
	public void setInstance(@Nullable OpeningInstance instance) {
		this.instance = instance;
		setChanged();
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
			instance = OpeningInstanceNbtCodec.read(input);
		} else {
			instance = null;
		}
	}
}
