package dev.aperture.client.render;

import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.runtime.material.MaterialBindingBuilder;
import dev.aperture.math.Transform3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.render.data.RenderDocument;
import dev.aperture.geometry.material.MaterialBindingSet;
import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshBakeService;
import dev.aperture.render.mesh.SolidShapeMeshCompiler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side cache of baked mesh assets for committed opening instances.
 */
public final class InstanceRenderCache {
	private static final InstanceRenderCache INSTANCE = new InstanceRenderCache();
	private static final MeshBakeService BAKE_SERVICE = new MeshBakeService(new SolidShapeMeshCompiler());

	private final Map<UUID, CachedInstanceRender> cache = new HashMap<>();

	private InstanceRenderCache() {
	}

	public static InstanceRenderCache get() {
		return INSTANCE;
	}

	public CachedInstanceRender resolve(OpeningInstance instance) {
		ApertureRuntime runtime = ApertureRuntime.get();
		CachedInstanceRender cached = cache.computeIfAbsent(
			instance.instanceId(),
			id -> new CachedInstanceRender(RenderDocument.forInstance(id))
		);

		PipelineResult pipeline = runtime.generation().generatePipeline(instance);
		GeometryResult geometry = pipeline.geometry();
		RenderDelta delta = cached.document().updateFrom(geometry);
		if (!delta.isEmpty() || cached.meshAsset().partIds().isEmpty()) {
			cached.meshAsset = BAKE_SERVICE.applyDeltaFromAssembly(
				cached.document(),
				cached.meshAsset(),
				delta,
				pipeline,
				LODLevel.FULL
			);
		}

		ParameterSet mergedParameters = ParameterSet.mergeDefaults(
			runtime.openingTypes().require(instance.typeId()).parameters(),
			instance.parameters()
		);
		if (!mergedParameters.equals(cached.lastParameters)) {
			cached.materialBindings = MaterialBindingBuilder.build(
				runtime.openingTypes().require(instance.typeId()),
				instance,
				geometry,
				runtime.materials()
			);
			cached.lastParameters = mergedParameters;
		}

		cached.transform = instance.transform();
		return cached;
	}

	public void invalidate(UUID instanceId) {
		cache.remove(instanceId);
	}

	public void clear() {
		cache.clear();
	}

	public static final class CachedInstanceRender {
		private final RenderDocument document;
		private MeshAsset meshAsset = MeshAsset.empty(LODLevel.FULL);
		private MaterialBindingSet materialBindings = new MaterialBindingSet(Map.of());
		private ParameterSet lastParameters = ParameterSet.empty();
		private Transform3d transform = Transform3d.at(0, 0, 0, dev.aperture.math.Facing.NORTH);

		private CachedInstanceRender(RenderDocument document) {
			this.document = document;
		}

		public RenderDocument document() {
			return document;
		}

		public MeshAsset meshAsset() {
			return meshAsset;
		}

		public MaterialBindingSet materialBindings() {
			return materialBindings;
		}

		public Transform3d transform() {
			return transform;
		}
	}
}
