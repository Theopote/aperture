package dev.aperture.client.render.placement;

import dev.aperture.api.ApertureApi;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.render.data.PreviewRenderContext;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.render.mesh.BoxMeshCompiler;
import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshBakeService;

import java.util.Optional;
import java.util.UUID;

/**
 * Client-side bridge from placement preview to the pure-Java render pipeline.
 */
public final class PlacementPreviewMeshService {
	private static final MeshBakeService BAKE_SERVICE = new MeshBakeService(new BoxMeshCompiler());

	private static PreviewRenderContext context;
	private static MeshAsset meshAsset = MeshAsset.empty(LODLevel.FULL);
	private static String currentHostAnchor;

	private PlacementPreviewMeshService() {
	}

	public static void update(PlacementSession session) {
		try {
			String hostAnchor = session.targetHost().anchor();
			if (context == null || !hostAnchor.equals(currentHostAnchor)) {
				context = new PreviewRenderContext(previewToken(hostAnchor));
				context.bind(session);
				meshAsset = MeshAsset.empty(LODLevel.FULL);
				currentHostAnchor = hostAnchor;
			}

			GeometryResult geometry = ApertureApi.get().generation().generate(session.previewInstance());
			RenderDelta delta = context.updateGeometry(geometry);
			meshAsset = BAKE_SERVICE.applyDelta(context.document(), meshAsset, delta, LODLevel.FULL);
		} catch (IllegalStateException notInitialized) {
			clear();
		} catch (RuntimeException exception) {
			clear();
			throw exception;
		}
	}

	public static Optional<PreviewRenderContext> context() {
		return Optional.ofNullable(context);
	}

	public static MeshAsset meshAsset() {
		return meshAsset;
	}

	public static void clear() {
		context = null;
		meshAsset = MeshAsset.empty(LODLevel.FULL);
		currentHostAnchor = null;
	}

	private static UUID previewToken(String hostAnchor) {
		return UUID.nameUUIDFromBytes(
			("placement-preview:" + hostAnchor).getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);
	}
}
