package dev.aperture.render.mesh;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.model.PartId;
import dev.aperture.render.data.RenderDelta;
import dev.aperture.render.data.RenderDocument;
import dev.aperture.render.data.RenderPart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compiles only dirty mesh parts and patches the existing asset.
 */
public final class MeshBakeService {
	private final MeshCompiler compiler;

	public MeshBakeService(MeshCompiler compiler) {
		this.compiler = Objects.requireNonNull(compiler, "compiler");
	}

	public MeshAsset bakeAll(GeometryResult geometry, LODLevel level) {
		Map<PartId, MeshSection> sections = new HashMap<>();
		for (GeometrySolid solid : geometry.solids()) {
			MeshSection section = compiler.compile(solid, level);
			sections.put(section.partId(), section);
		}
		return new MeshAsset(level, sections, geometry.bounds());
	}

	public MeshAsset bakeAll(PipelineResult pipeline, LODLevel level) {
		return bakeAllFromAssembly(pipeline.geometry(), pipeline.meshes(), level);
	}

	public MeshAsset bakeAllFromAssembly(GeometryResult geometry, MeshAssembly assembly, LODLevel level) {
		Map<PartId, MeshSection> sections = new HashMap<>();
		for (GeometrySolid solid : geometry.solids()) {
			var mesh = assembly.partsByPath().get(solid.componentPath());
			if (mesh == null) {
				throw new IllegalStateException("Missing pre-baked mesh for: " + solid.componentPath());
			}
			MeshSection section = MeshSectionFactory.fromMesh(PartId.of(solid.componentPath()), solid.layer(), mesh);
			sections.put(section.partId(), section);
		}
		return new MeshAsset(level, sections, assembly.bounds());
	}

	public MeshAsset bakeDirty(RenderDocument document, MeshAsset existing, Set<PartId> dirtyParts, LODLevel level) {
		Objects.requireNonNull(document, "document");
		Objects.requireNonNull(existing, "existing");
		Objects.requireNonNull(dirtyParts, "dirtyParts");

		if (dirtyParts.isEmpty()) {
			return existing;
		}

		Map<PartId, MeshSection> updated = new HashMap<>();
		for (PartId partId : dirtyParts) {
			RenderPart part = document.parts().get(partId)
				.orElseThrow(() -> new IllegalStateException("Missing render part for dirty id: " + partId.componentPath()));
			MeshSection section = compiler.compile(part.solid(), level);
			part.setMeshHandle(section.handle());
			updated.put(partId, section);
		}

		return MeshAsset.patch(existing, updated, Set.of());
	}

	public MeshAsset applyDelta(
		RenderDocument document,
		MeshAsset existing,
		RenderDelta delta,
		LODLevel level
	) {
		Map<PartId, MeshSection> updated = new HashMap<>();
		for (PartId partId : delta.dirty()) {
			RenderPart part = document.parts().get(partId)
				.orElseThrow(() -> new IllegalStateException("Missing render part for dirty id: " + partId.componentPath()));
			MeshSection section = compiler.compile(part.solid(), level);
			part.setMeshHandle(section.handle());
			updated.put(partId, section);
		}
		return MeshAsset.patch(existing, updated, delta.removed());
	}

	public MeshAsset applyDeltaFromAssembly(
		RenderDocument document,
		MeshAsset existing,
		RenderDelta delta,
		PipelineResult pipeline,
		LODLevel level
	) {
		return applyDeltaFromAssembly(document, existing, delta, pipeline.geometry(), pipeline.meshes(), level);
	}

	public MeshAsset applyDeltaFromAssembly(
		RenderDocument document,
		MeshAsset existing,
		RenderDelta delta,
		GeometryResult geometry,
		MeshAssembly assembly,
		LODLevel level
	) {
		Map<String, GeometrySolid> solidsByPath = new HashMap<>();
		for (GeometrySolid solid : geometry.solids()) {
			solidsByPath.put(solid.componentPath(), solid);
		}

		Map<PartId, MeshSection> updated = new HashMap<>();
		for (PartId partId : delta.dirty()) {
			String path = partId.componentPath();
			var mesh = assembly.partsByPath().get(path);
			if (mesh == null) {
				throw new IllegalStateException("Missing pre-baked mesh for: " + path);
			}
			GeometrySolid solid = solidsByPath.get(path);
			if (solid == null) {
				throw new IllegalStateException("Missing geometry solid for: " + path);
			}
			RenderPart part = document.parts().get(partId)
				.orElseThrow(() -> new IllegalStateException("Missing render part for dirty id: " + path));
			MeshSection section = MeshSectionFactory.fromMesh(partId, solid.layer(), mesh);
			part.setMeshHandle(section.handle());
			updated.put(partId, section);
		}
		return MeshAsset.patch(existing, updated, delta.removed());
	}
}
