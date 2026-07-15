package dev.aperture.geometry.generator;

import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;

/**
 * Procedural generator for an opening type. Implementations live in
 * {@code aperture-geometry} or addon mods; register via {@link dev.aperture.api.registry.GeneratorRegistry}.
 */
public interface OpeningGenerator {
	String id();

	/**
	 * Runs the full opening generator pipeline including mesh assembly.
	 */
	PipelineResult generate(GenerationContext context);

	default GeometryResult generateGeometry(GenerationContext context) {
		return generate(context).geometry();
	}
}
