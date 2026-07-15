package dev.aperture.core.definition;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.OpeningComponent;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parametric.ParametricSchema;
import dev.aperture.core.parametric.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable procedural recipe for an opening family (Revit "family").
 * Opening types are defined as a {@link ComponentAssembly} — window, door, and curtain wall
 * differ only by which components are included.
 */
public record OpeningTypeDefinition(
	int schemaVersion,
	OpeningId id,
	OpeningCategory category,
	ParametricSchema parametricSchema,
	List<ConstraintRule> constraints,
	GeneratorId generator,
	ComponentAssembly components,
	List<String> materialSlots
) {
	public OpeningTypeDefinition {
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be >= 1");
		}
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(parametricSchema, "parametricSchema");
		Objects.requireNonNull(generator, "generator");
		Objects.requireNonNull(components, "components");
		constraints = List.copyOf(constraints);
		materialSlots = List.copyOf(materialSlots);
	}

	public Map<String, ParameterDefinition> parameters() {
		return parametricSchema.toLegacyMap();
	}

	public static Builder builder(OpeningId id, OpeningCategory category, GeneratorId generator) {
		return new Builder(id, category, generator);
	}

	public static final class Builder {
		private final OpeningId id;
		private final OpeningCategory category;
		private final GeneratorId generator;
		private int schemaVersion = 1;
		private final ParametricSchema.Builder parametricSchema = ParametricSchema.builder();
		private final List<ConstraintRule> constraints = new ArrayList<>();
		private final List<OpeningComponent> components = new ArrayList<>();
		private final List<String> materialSlots = new ArrayList<>();

		private Builder(OpeningId id, OpeningCategory category, GeneratorId generator) {
			this.id = id;
			this.category = category;
			this.generator = generator;
		}

		public Builder parameter(String name, Parameter parameter) {
			parametricSchema.put(name, parameter);
			return this;
		}

		public Builder parameter(String name, ParameterDefinition definition) {
			parametricSchema.put(name, dev.aperture.core.parametric.ParameterBridge.fromDefinition(definition));
			return this;
		}

		public Builder constraint(String expression, String message) {
			constraints.add(new ConstraintRule(expression, message));
			return this;
		}

		public Builder component(OpeningComponent component) {
			components.add(component);
			return this;
		}

		public Builder components(ComponentAssembly assembly) {
			components.addAll(assembly.all());
			return this;
		}

		/** @deprecated Prefer {@link #component(OpeningComponent)} or {@link #components(ComponentAssembly)} */
		@Deprecated
		public Builder component(String name, Object value) {
			Map<String, Object> legacy = new LinkedHashMap<>();
			legacy.put(name, value);
			components.addAll(ComponentAssembly.fromLegacyMap(legacy).all());
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
				parametricSchema.build(),
				constraints,
				generator,
				ComponentAssembly.of(components),
				materialSlots
			);
		}
	}
}
