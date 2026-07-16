package dev.aperture.pipeline.stage;

import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.pipeline.CacheFingerprint;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageCacheKey;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageId;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;
import java.util.Optional;

/** Bakes geometry into meshes without rebuilding parameters, plans, or solids. */
public final class MeshStage implements PipelineStage<GeometryStage.GeometryCompilation, MeshAssembly> {
	public static final String COMPILER_VERSION = "opening-mesh-v1";

	private final OpeningMeshCompiler compiler;

	public MeshStage(OpeningMeshCompiler compiler) {
		this.compiler = Objects.requireNonNull(compiler, "compiler cannot be null");
	}

	@Override
	public String name() { return id().externalName(); }

	@Override
	public StageId id() { return StageId.MESH; }

	@Override
	public Class<?> inputType() { return GeometryStage.GeometryCompilation.class; }

	@Override
	public Class<?> outputType() { return MeshAssembly.class; }

	@Override
	public Optional<StageCacheKey> cacheKey(GeometryStage.GeometryCompilation input, StageContext ctx) {
		return Optional.of(new StageCacheKey(
			id(),
			GeometryStage.PIPELINE_VERSION,
			input.openingTypeId(),
			input.definitionFingerprint(),
			input.parameterFingerprint() + ":" + CacheFingerprint.text(input.geometry().recipe()),
			input.assetRevision(),
			COMPILER_VERSION,
			"default"
		));
	}

	@Override
	public StageResult<MeshAssembly> execute(GeometryStage.GeometryCompilation input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			return new StageResult.Success<>(compiler.compile(input.geometry().result()));
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to compile mesh: " + exception.getMessage(), exception);
		}
	}
}