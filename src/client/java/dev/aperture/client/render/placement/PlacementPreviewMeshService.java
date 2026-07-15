package dev.aperture.client.render.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.api.ApertureApi;
import dev.aperture.api.material.MaterialBindingBuilder;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.ClientMaterialPreview;
import dev.aperture.client.render.FabricRenderBackend;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.McUnits;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.render.data.PreviewRenderContext;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.render.material.MaterialBindingSet;
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
	private static MaterialBindingSet materialBindings = new MaterialBindingSet(java.util.Map.of());
	private static ParameterSet lastParameters = ParameterSet.empty();
	private static String currentHostAnchor;

	private PlacementPreviewMeshService() {
	}

	public static void update(PlacementSession session) {
		try {
			ApertureApi api = ApertureApi.get();
			String hostAnchor = session.targetHost().anchor();
			if (context == null || !hostAnchor.equals(currentHostAnchor)) {
				context = new PreviewRenderContext(previewToken(hostAnchor));
				context.bind(session);
				meshAsset = MeshAsset.empty(LODLevel.FULL);
				lastParameters = ParameterSet.empty();
				currentHostAnchor = hostAnchor;
			}

			GeometryResult geometry = api.generation().generate(session.previewInstance());
			RenderDelta delta = context.updateGeometry(geometry);
			meshAsset = BAKE_SERVICE.applyDelta(context.document(), meshAsset, delta, LODLevel.FULL);

			var definition = api.openingTypes().require(session.selectedTypeId());
			ParameterSet mergedParameters = ParameterSet.mergeDefaults(definition.parameters(), session.parameterOverrides());
			if (!mergedParameters.equals(lastParameters)) {
				materialBindings = MaterialBindingBuilder.build(
					definition,
					session.previewInstance(),
					geometry,
					api.materials()
				);
				lastParameters = mergedParameters;
			}
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

	public static MaterialBindingSet materialBindings() {
		return materialBindings.forPreviewMode(ClientMaterialPreview.mode());
	}

	public static void clear() {
		context = null;
		meshAsset = MeshAsset.empty(LODLevel.FULL);
		materialBindings = new MaterialBindingSet(java.util.Map.of());
		lastParameters = ParameterSet.empty();
		currentHostAnchor = null;
	}

	private static UUID previewToken(String hostAnchor) {
		return UUID.nameUUIDFromBytes(
			("placement-preview:" + hostAnchor).getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);
	}
}
