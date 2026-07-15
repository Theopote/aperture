package dev.aperture.block;

import com.mojang.serialization.MapCodec;
import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.registry.ApertureBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Invisible marker block that anchors one {@link OpeningBlockEntity} per placed opening.
 */
public final class OpeningBlock extends BaseEntityBlock {
	public static final MapCodec<OpeningBlock> CODEC = simpleCodec(OpeningBlock::new);

	public OpeningBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new OpeningBlockEntity(ApertureBlockEntities.OPENING, pos, state);
	}
}
