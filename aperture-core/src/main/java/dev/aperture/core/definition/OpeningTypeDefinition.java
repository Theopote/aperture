package dev.aperture.core.definition;

import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable procedural recipe for an opening family (Revit "family").
 */
public record OpeningTypeDefinition(
	int schemaVersion,
	OpeningId id,
	OpeningCategory category,
	Map<String, ParameterDefinition> parameters,
	List<ConstraintRule> constraints,
	GeneratorId generator,
	Map<String, Object> components,
	List<String> materialSlots
) {
	public OpeningTypeDefinition {
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be >= 1");
		}
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(generator, "generator");
		parameters = Map.copyOf(parameters);
		constraints = List.copyOf(constraints);
		components = Map.copyOf(components);
		materialSlots = List.copyOf(materialSlots);
	}

	public static Builder builder(OpeningId id, OpeningCategory category, GeneratorId generator) {
		return new Builder(id, category, generator);
	}

	public static final class Builder {
		private final OpeningId id;
		private final OpeningCategory category;
		private final GeneratorId generator;
		private int schemaVersion = 1;
		private final Map<String, ParameterDefinition> parameters = new LinkedHashMap<>();
		private final List<ConstraintRule> constraints = new java.util.ArrayList<>();
		private final Map<String, Object> components = new LinkedHashMap<>();
		private final List<String> materialSlots = new java.util.ArrayList<>();

		private Builder(OpeningId id, OpeningCategory category, GeneratorId generator) {
			this.id = id;
			this.category = category;
			this.generator = generator;
		}

		public Builder parameter(String name, ParameterDefinition definition) {
			parameters.put(name, definition);
			return this;
		}

		public Builder constraint(String expression, String message) {
			constraints.add(new ConstraintRule(expression, message));
			return this;
		}

		public Builder component(String name, Object value) {
			components.put(name, value);
			return this;
		}

		public Builder materialSlot(String slot) {
			materialSlots.add(slot);
			return this;
		}

		public OpeningTypeDefinition build() {
			return new OpeningTypeDefinition(
				schemaVersion,
				id,
				category,
				parameters,
				constraints,
				generator,
				components,
				materialSlots
			);
		}
	}
}
