package dev.aperture.client.render.placement;

import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.FabricRenderBackend;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.fabric.placement.McUnits;
import dev.aperture.render.material.MaterialBindingSet;
import dev.aperture.render.mesh.MeshAsset;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

/**
 * Renders placement ghost meshes through the material-aware Fabric backend.
 */
public final class GhostPreviewMeshRenderer {
	private static final FabricRenderBackend BACKEND = FabricRenderBackend.INSTANCE;

	private GhostPreviewMeshRenderer() {
	}

	public static void emit(LevelRenderContext context) {
		ClientPlacementPreview.session().ifPresent(session -> {
			MeshAsset meshAsset = PlacementPreviewMeshService.meshAsset();
			MaterialBindingSet bindings = PlacementPreviewMeshService.materialBindings();
			if (meshAsset.partIds().isEmpty() || bindings.partIds().isEmpty()) {
				return;
			}

			Transform3d transform = session.previewInstance().transform();
			BlockPos blockPos = blockPosFor(transform);
			Minecraft client = Minecraft.getInstance();
			if (client.level == null) {
				return;
			}

			int lightCoords = LevelRenderer.getLightCoords(client.level, blockPos);
			BACKEND.submitAssetGhost(
				context.poseStack(),
				context.submitNodeCollector(),
				lightCoords,
				meshAsset,
				bindings,
				transform,
				blockPos,
				session.isValid()
			);
		});
	}

	private static BlockPos blockPosFor(Transform3d transform) {
		Vec3d origin = transform.origin();
		return BlockPos.containing(
			origin.x() / McUnits.MILLIMETERS_PER_BLOCK,
			origin.y() / McUnits.MILLIMETERS_PER_BLOCK,
			origin.z() / McUnits.MILLIMETERS_PER_BLOCK
		);
	}
}
