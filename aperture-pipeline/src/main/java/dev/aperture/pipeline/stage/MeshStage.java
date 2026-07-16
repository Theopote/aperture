package dev.aperture.pipeline.stage;

import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.opening.geometry.build.CompiledGeometry;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Bakes geometry into meshes without rebuilding parameters, plans, or solids. */
public final class MeshStage implements PipelineStage<CompiledGeometry, MeshAssembly> {
	private final OpeningMeshCompiler compiler;

	public MeshStage(OpeningMeshCompiler compiler) {
		this.compiler = Objects.requireNonNull(compiler, "compiler cannot be null");
	}

	@Override
	public String name() {
		return "mesh";
	}

	@Override
	public StageResult<MeshAssembly> execute(CompiledGeometry input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			return new StageResult.Success<>(compiler.compile(input.result()));
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to compile mesh: " + exception.getMessage(), exception);
		}
	}
}