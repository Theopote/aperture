package dev.aperture.pipeline.stage;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Map;
import java.util.Objects;

/** Resolves an opening type and validates raw parameter overrides. */
public final class DefinitionStage implements PipelineStage<Object, ParameterStage.ResolvedDefinition> {
	private final OpeningTypeRegistry registry;

	public DefinitionStage() {
		this(defaultRegistry());
	}

	public DefinitionStage(OpeningTypeRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry cannot be null");
	}

	private static OpeningTypeRegistry defaultRegistry() {
		OpeningTypeRegistry registry = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);
		return registry;
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

		String typeId = normalizeLegacyTypeId(request.typeId());
		OpeningTypeDefinition typeDefinition;
		try {
			typeDefinition = registry.get(OpeningId.parse(typeId)).orElse(null);
		} catch (IllegalArgumentException exception) {
			return new StageResult.Failure<>("Invalid opening type ID: " + request.typeId(), exception);
		}
		if (typeDefinition == null) {
			return new StageResult.Failure<>("Unknown opening type: " + request.typeId());
		}

		ParametricEditor editor = ParametricEditor.fromDefinition(typeDefinition, ParameterSet.empty());
		var patchResult = editor.patch(request.userParameters());
		if (!patchResult.success()) {
			return new StageResult.Failure<>("Invalid opening parameters: " + patchResult.issues());
		}
		return new StageResult.Success<>(
			new ParameterStage.ResolvedDefinition(typeDefinition, editor.overridesOnly())
		);
	}

	private static String normalizeLegacyTypeId(String typeId) {
		return switch (typeId) {
			case "aperture:door_standard" -> "aperture:door";
			case "aperture:window_standard" -> "aperture:fixed_window";
			default -> typeId;
		};
	}

	public record OpeningRequest(String typeId, Map<String, Object> userParameters) {
		public OpeningRequest {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
		}

		public static OpeningRequest of(String typeId) {
			return new OpeningRequest(typeId, Map.of());
		}
	}
}
