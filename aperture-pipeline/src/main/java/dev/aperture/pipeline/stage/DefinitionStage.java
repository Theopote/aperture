package dev.aperture.pipeline.stage;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Definition resolution stage.
 * <p>
 * Looks up the opening type definition from the registry using the type ID.
 * <p>
 * Input: {@link OpeningDefinition} (type ID + user parameters)
 * Output: {@link ParameterStage.ResolvedDefinition} (type definition + user parameters)
 */
public final class DefinitionStage implements PipelineStage<OpeningDefinition, ParameterStage.ResolvedDefinition> {

	private final OpeningTypeRegistry registry;

	/**
	 * Create definition stage with given registry.
	 */
	public DefinitionStage(OpeningTypeRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry cannot be null");
	}

	@Override
	public String name() {
		return "definition";
	}

	@Override
	public StageResult<ParameterStage.ResolvedDefinition> execute(OpeningDefinition input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Looking up opening type: " + input.typeId());

		// Lookup type definition
		OpeningTypeDefinition typeDef = registry.lookup(input.typeId());

		if (typeDef == null) {
			return new StageResult.Failure<>(
				"Unknown opening type: " + input.typeId()
			);
		}

		ctx.debug("Found opening type: " + typeDef.id());

		return new StageResult.Success<>(
			new ParameterStage.ResolvedDefinition(typeDef, input.userParameters())
		);
	}

	/**
	 * Input for DefinitionStage.
	 * Minimal opening definition with type ID and user parameters.
	 */
	public record OpeningDefinition(
		String typeId,
		ParameterSet userParameters
	) {
		public OpeningDefinition {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
		}

		/**
		 * Create definition with empty parameters.
		 */
		public static OpeningDefinition of(String typeId) {
			return new OpeningDefinition(typeId, ParameterSet.empty());
		}
	}
}
