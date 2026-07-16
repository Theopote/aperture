package dev.aperture.pipeline.stage;

import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.geometry.build.CompiledGeometry;
import dev.aperture.pipeline.CacheFingerprint;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageCacheKey;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageId;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;
import java.util.Optional;

/** Compiles the existing definition, parameters, and component plan into geometry. */
public final class GeometryStage implements PipelineStage<ComponentStage.PlannedOpening, GeometryStage.GeometryCompilation> {
	public static final String COMPILER_VERSION = "opening-geometry-v1";
	public static final String PIPELINE_VERSION = "kernel-pipeline-v1";

	private final OpeningGeometryCompiler compiler;
	private final ProfileCatalogRegistry profiles;

	public GeometryStage(OpeningGeometryCompiler compiler, ProfileCatalogRegistry profiles) {
		this.compiler = Objects.requireNonNull(compiler, "compiler cannot be null");
		this.profiles = Objects.requireNonNull(profiles, "profiles cannot be null");
	}

	@Override
	public String name() { return id().externalName(); }

	@Override
	public StageId id() { return StageId.GEOMETRY; }

	@Override
	public Class<?> inputType() { return ComponentStage.PlannedOpening.class; }

	@Override
	public Class<?> outputType() { return GeometryCompilation.class; }

	@Override
	public Optional<StageCacheKey> cacheKey(ComponentStage.PlannedOpening input, StageContext ctx) {
		return Optional.of(new StageCacheKey(
			id(),
			PIPELINE_VERSION,
			input.typeDefinition().id().toString(),
			CacheFingerprint.definition(input.typeDefinition()),
			CacheFingerprint.parameters(input.parameters()),
			profiles.revision(),
			COMPILER_VERSION,
			"default"
		));
	}

	@Override
	public StageResult<GeometryCompilation> execute(ComponentStage.PlannedOpening input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			long definitionFingerprint = CacheFingerprint.definition(input.typeDefinition());
			String parameterFingerprint = CacheFingerprint.parameters(input.parameters());
			CompiledGeometry geometry = compiler.compile(
				input.typeDefinition(), input.parameters(), input.plan(), profiles
			);
			return new StageResult.Success<>(new GeometryCompilation(
				input.typeDefinition().id().toString(),
				definitionFingerprint,
				parameterFingerprint,
				profiles.revision(),
				geometry
			));
		} catch (Exception exception) {
			return new StageResult.Failure<>(dev.aperture.pipeline.DiagnosticCode.GEOMETRY_COMPILATION_FAILED, "Failed to compile geometry: " + exception.getMessage(), exception);
		}
	}

	public record GeometryCompilation(
		String openingTypeId,
		long definitionFingerprint,
		String parameterFingerprint,
		long assetRevision,
		CompiledGeometry geometry
	) {
		public GeometryCompilation {
			Objects.requireNonNull(openingTypeId, "openingTypeId cannot be null");
			Objects.requireNonNull(parameterFingerprint, "parameterFingerprint cannot be null");
			Objects.requireNonNull(geometry, "geometry cannot be null");
		}
	}
}