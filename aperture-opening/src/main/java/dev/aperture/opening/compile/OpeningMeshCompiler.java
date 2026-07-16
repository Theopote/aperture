package dev.aperture.opening.compile;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.opening.mesh.MeshBuilder;

import java.util.Objects;

/** Bakes evaluated opening geometry into a CPU mesh assembly. */
public final class OpeningMeshCompiler {
	private final MeshBuilder meshBuilder;

	public OpeningMeshCompiler() {
		this(new MeshBuilder());
	}

	public OpeningMeshCompiler(MeshBuilder meshBuilder) {
		this.meshBuilder = Objects.requireNonNull(meshBuilder, "meshBuilder cannot be null");
	}

	public MeshAssembly compile(GeometryResult geometry) {
		return meshBuilder.build(Objects.requireNonNull(geometry, "geometry cannot be null"));
	}
}