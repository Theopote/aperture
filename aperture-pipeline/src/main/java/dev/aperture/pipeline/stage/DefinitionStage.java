package dev.aperture.pipeline.stage;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Map;
import java.util.Objects;

/**
 * Definition resolution stage.
 * <p>
 * Looks up the opening type definition from the registry using the type ID.
 * <p>
 * Input: {@link OpeningRequest} (type ID + raw user parameters)
 * Output: {@link ParameterStage.ResolvedDefinition} (type definition + user parameters)
 */
public final class DefinitionStage implements PipelineStage<Object, ParameterStage.ResolvedDefinition> {

	private final OpeningTypeRegistry registry;

	/**
	 * Create definition stage with default registry.
	 */
	public DefinitionStage() {
		this(OpeningTypeRegistry.getInstance());
	}

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
	public StageResult<ParameterStage.ResolvedDefinition> execute(Object input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		if (!(input instanceof OpeningRequest request)) {
			return new StageResult.Failure<>(
				"DefinitionStage requires OpeningRequest input but got: " + input.getClass().getSimpleName()
			);
		}

		ctx.debug("Looking up opening type: " + request.typeId());

		// Lookup type definition
		OpeningTypeDefinition typeDef = registry.lookup(request.typeId());

		if (typeDef == null) {
			return new StageResult.Failure<>(
				"Unknown opening type: " + request.typeId()
			);
		}

		ctx.debug("Found opening type: " + typeDef.id());

		ParametricEditor editor = ParametricEditor.fromDefinition(typeDef, ParameterSet.empty());
		var patchResult = editor.patch(request.userParameters());
		if (!patchResult.success()) {
			return new StageResult.Failure<>(
				"Invalid opening parameters: " + patchResult.issues()
			);
		}

		return new StageResult.Success<>(
			new ParameterStage.ResolvedDefinition(typeDef, editor.overridesOnly())
		);
	}

	/**
	 * Input for DefinitionStage.
	 * Raw opening request with type ID and user parameters.
	 */
	public record OpeningRequest(
		String typeId,
		Map<String, Object> userParameters
	) {
		public OpeningRequest {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
		}

		/**
		 * Create definition with empty parameters.
		 */
		public static OpeningRequest of(String typeId) {
			return new OpeningRequest(typeId, Map.of());
		}
	}
}
