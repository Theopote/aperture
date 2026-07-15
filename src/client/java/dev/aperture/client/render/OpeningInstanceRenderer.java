package dev.aperture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.block.entity.OpeningBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders committed opening instances through the Aperture render pipeline.
 */
public final class OpeningInstanceRenderer implements BlockEntityRenderer<OpeningBlockEntity, OpeningInstanceRenderState> {
	private final FabricRenderBackend backend = FabricRenderBackend.INSTANCE;

	public OpeningInstanceRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public OpeningInstanceRenderState createRenderState() {
		return new OpeningInstanceRenderState();
	}

	@Override
	public void extractRenderState(
		OpeningBlockEntity blockEntity,
		OpeningInstanceRenderState state,
		float tickProgress,
		Vec3 cameraPos,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
		blockEntity.resolveInstance().ifPresentOrElse(
			instance -> state.setFrom(InstanceRenderCache.get().resolve(instance)),
			state::clear
		);
	}

	@Override
	public void submit(
		OpeningInstanceRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector queue,
		CameraRenderState camera
	) {
		if (!state.hasMesh()) {
			return;
		}

		poseStack.pushPose();
		backend.submitAsset(
			poseStack,
			queue,
			state.lightCoords,
			state.meshAsset(),
			state.materialBindings(),
			state.transform(),
			state.blockPos
		);
		poseStack.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}
}
