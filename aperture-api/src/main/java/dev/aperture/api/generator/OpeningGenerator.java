package dev.aperture.api.generator;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryResult;

/**
 * Procedural generator for an opening type. Addon mods implement this
 * and register via {@link dev.aperture.api.registry.GeneratorRegistry}.
 */
public interface OpeningGenerator {
	String id();

	GeometryResult generate(OpeningTypeDefinition definition, ParameterSet parameters);
}
