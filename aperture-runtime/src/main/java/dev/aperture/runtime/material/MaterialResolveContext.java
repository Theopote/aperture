package dev.aperture.runtime.material;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.geometry.model.GeometryLayer;

import java.util.Objects;

/**
 * Context passed to a {@link MaterialResolver} when resolving a material slot.
 */
public record MaterialResolveContext(
	String materialSlot,
	GeometryLayer layer,
	OpeningId typeId,
	OpeningTypeDefinition definition,
	ParameterSet parameters
) {
	public MaterialResolveContext {
		Objects.requireNonNull(materialSlot, "materialSlot");
		Objects.requireNonNull(layer, "layer");
		Objects.requireNonNull(typeId, "typeId");
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(parameters, "parameters");
	}
}
