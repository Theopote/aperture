package dev.aperture.geometry.generator;

import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;

/**
 * Procedural generator for an opening type. Implementations live in
 * {@code aperture-geometry} or addon mods; register via {@link dev.aperture.api.registry.GeneratorRegistry}.
 */
public interface OpeningGenerator {
	String id();

	GeometryResult generate(GenerationContext context);
}
