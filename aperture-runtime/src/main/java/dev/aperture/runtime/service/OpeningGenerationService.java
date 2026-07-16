package dev.aperture.runtime.service;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.geometry.export.GeometryExport;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import dev.aperture.kernel.ApertureKernel;
import dev.aperture.kernel.OpeningRequest;
import dev.aperture.kernel.OpeningResult;
import dev.aperture.runtime.mapper.OpeningInstanceRequestMapper;

import java.util.Objects;

/** Runtime bridge that delegates every generation request to {@link ApertureKernel}. */
public final class OpeningGenerationService {
	private final ApertureKernel kernel;
	private final OpeningInstanceRequestMapper requestMapper;

	public OpeningGenerationService(ApertureKernel kernel) {
		this(kernel, new OpeningInstanceRequestMapper());
	}

	public OpeningGenerationService(
		ApertureKernel kernel,
		OpeningInstanceRequestMapper requestMapper
	) {
		this.kernel = Objects.requireNonNull(kernel, "kernel cannot be null");
		this.requestMapper = Objects.requireNonNull(requestMapper, "requestMapper cannot be null");
	}

	public OpeningResult generate(OpeningRequest request) {
		return kernel.generate(request);
	}

	public OpeningResult generate(OpeningInstance instance) {
		return generate(requestMapper.map(instance));
	}

	public GeometryRecipe compileRecipe(OpeningInstance instance) {
		return requireOutput(instance).recipe();
	}

	public String exportRecipeJson(OpeningInstance instance) {
		return GeometryRecipeCodec.toJson(compileRecipe(instance));
	}

	public String exportGltf(OpeningInstance instance) {
		return GeometryExport.toGltf(compileRecipe(instance));
	}

	private PipelineResult requireOutput(OpeningInstance instance) {
		OpeningResult result = generate(instance);
		if (result instanceof OpeningResult.Success success) {
			return success.output();
		}
		OpeningResult.Failure failure = result.asFailure();
		throw new IllegalStateException(
			"Opening generation failed at " + failure.failedStage() + ": " + failure.errorMessage(),
			failure.cause()
		);
	}
}