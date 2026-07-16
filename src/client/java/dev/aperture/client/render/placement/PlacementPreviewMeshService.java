package dev.aperture.client.render.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.runtime.material.MaterialBindingBuilder;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.ClientMaterialPreview;
import dev.aperture.client.render.FabricRenderBackend;
import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.McUnits;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.render.data.PreviewRenderContext;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.geometry.material.MaterialBindingSet;
import dev.aperture.render.material.MaterialBindingFilters;
import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshBakeService;
import dev.aperture.render.mesh.SolidShapeMeshCompiler;

import java.util.Optional;
import java.util.UUID;

/**
 * Client-side bridge from placement preview to the pure-Java render pipeline.
 */
public final class PlacementPreviewMeshService {
	private static final MeshBakeService BAKE_SERVICE = new MeshBakeService(new SolidShapeMeshCompiler());

	private static PreviewRenderContext context;
	private static MeshAsset meshAsset = MeshAsset.empty(LODLevel.FULL);
	private static MaterialBindingSet materialBindings = new MaterialBindingSet(java.util.Map.of());
	private static ParameterSet lastParameters = ParameterSet.empty();
	private static String currentHostAnchor;

	private PlacementPreviewMeshService() {
	}

	public static void update(PlacementSession session) {
		try {
			ApertureRuntime runtime = ApertureRuntime.get();
			String hostAnchor = session.targetHost().anchor();
			if (context == null || !hostAnchor.equals(currentHostAnchor)) {
				context = new PreviewRenderContext(previewToken(hostAnchor));
				context.bind(session);
				meshAsset = MeshAsset.empty(LODLevel.FULL);
				lastParameters = ParameterSet.empty();
				currentHostAnchor = hostAnchor;
			}

			PipelineResult pipeline = runtime.generation().generate(session.previewInstance()).asSuccess().output();
			GeometryResult geometry = pipeline.geometry();
			RenderDelta delta = context.updateGeometry(geometry);
			meshAsset = BAKE_SERVICE.applyDeltaFromAssembly(context.document(), meshAsset, delta, pipeline, LODLevel.FULL);

			var definition = runtime.openingTypes().require(session.selectedTypeId());
			ParameterSet mergedParameters = definition.resolveParameters(session.parameterOverrides());
			if (!mergedParameters.equals(lastParameters)) {
				materialBindings = MaterialBindingBuilder.build(
					definition,
					session.previewInstance(),
					geometry,
					runtime.materials()
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
		return MaterialBindingFilters.forPreviewMode(materialBindings, ClientMaterialPreview.mode());
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
