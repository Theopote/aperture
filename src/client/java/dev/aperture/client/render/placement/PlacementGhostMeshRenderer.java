package dev.aperture.client.render.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.FabricRenderBackend;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.McUnits;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshSection;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

/**
 * Renders placement preview meshes through {@link FabricRenderBackend} and resolved materials.
 */
public final class PlacementGhostMeshRenderer {
	private PlacementGhostMeshRenderer() {
	}

	public static void emit(LevelRenderContext renderContext) {
		ClientPlacementPreview.session().ifPresent(session -> {
			MeshAsset asset = PlacementPreviewMeshService.meshAsset();
			if (asset.partIds().isEmpty()) {
				return;
			}

			Transform3d transform = session.previewInstance().transform();
			BlockPos anchor = previewAnchor(transform);
			int lightCoords = renderContext.minecraft().level != null
				? LevelRenderer.getLightCoords(renderContext.minecraft().level, anchor)
				: 15728880;

			PoseStack poseStack = renderContext.poseStack();
			poseStack.pushPose();
			poseStack.translate(
				anchor.getX() - renderContext.camera().position().x,
				anchor.getY() - renderContext.camera().position().y,
				anchor.getZ() - renderContext.camera().position().z
			);

			FabricRenderBackend.INSTANCE.submitGhostAsset(
				poseStack,
				renderContext.submitNodeCollector(),
				lightCoords,
				filteredAsset(asset, session),
				PlacementPreviewMeshService.materialBindings(),
				transform,
				anchor,
				session.isValid()
			);
			poseStack.popPose();
		});
	}

	private static MeshAsset filteredAsset(MeshAsset asset, PlacementSession session) {
		var bindings = PlacementPreviewMeshService.materialBindings();
		if (bindings.partIds().size() == asset.partIds().size()) {
			return asset;
		}

		java.util.Map<dev.aperture.render.data.PartId, MeshSection> filtered = new java.util.LinkedHashMap<>();
		for (MeshSection section : asset.sections().values()) {
			if (bindings.get(section.partId()).isPresent()) {
				filtered.put(section.partId(), section);
			}
		}
		return new MeshAsset(asset.level(), filtered, asset.bounds());
	}

	private static BlockPos previewAnchor(Transform3d transform) {
		return BlockPos.containing(
			transform.origin().x() / McUnits.MILLIMETERS_PER_BLOCK,
			transform.origin().y() / McUnits.MILLIMETERS_PER_BLOCK,
			transform.origin().z() / McUnits.MILLIMETERS_PER_BLOCK
		);
	}
}
