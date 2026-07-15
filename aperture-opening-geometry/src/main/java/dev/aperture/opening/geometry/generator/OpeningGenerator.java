package dev.aperture.opening.geometry.generator;

import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;

/**
 * Optional extension point for custom opening geometry strategies.
 */
public interface OpeningGenerator {
	String id();

	GeometryResult generateGeometry(GenerationContext context);

	default PipelineResult generate(GenerationContext context) {
		return OpeningGenerationPipeline.standard().generate(context);
	}
}
