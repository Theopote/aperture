package dev.aperture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.api.ApertureApi;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.render.data.RenderDocument;
import dev.aperture.render.mesh.BoxMeshCompiler;
import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshBakeService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side cache of baked mesh assets for committed opening instances.
 */
public final class InstanceRenderCache {
	private static final InstanceRenderCache INSTANCE = new InstanceRenderCache();
	private static final MeshBakeService BAKE_SERVICE = new MeshBakeService(new BoxMeshCompiler());

	private final Map<UUID, CachedInstanceRender> cache = new HashMap<>();

	private InstanceRenderCache() {
	}

	public static InstanceRenderCache get() {
		return INSTANCE;
	}

	public CachedInstanceRender resolve(OpeningInstance instance) {
		CachedInstanceRender cached = cache.computeIfAbsent(
			instance.instanceId(),
			id -> new CachedInstanceRender(RenderDocument.forInstance(id))
		);

		GeometryResult geometry = ApertureApi.get().generation().generate(instance);
		RenderDelta delta = cached.document().updateFrom(geometry);
		if (!delta.isEmpty() || cached.meshAsset().partIds().isEmpty()) {
			cached.meshAsset = BAKE_SERVICE.applyDelta(cached.document(), cached.meshAsset(), delta, LODLevel.FULL);
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
		private Transform3d transform = Transform3d.at(0, 0, 0, dev.aperture.core.geometry.Facing.NORTH);

		private CachedInstanceRender(RenderDocument document) {
			this.document = document;
		}

		public RenderDocument document() {
			return document;
		}

		public MeshAsset meshAsset() {
			return meshAsset;
		}

		public Transform3d transform() {
			return transform;
		}
	}
}
