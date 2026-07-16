package dev.aperture.pipeline.stage;

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
public final class DefinitionStage implements PipelineStage<DefinitionStage.OpeningRequest, ParameterStage.ResolvedDefinition> {
	private final OpeningTypeRegistry registry;


	public DefinitionStage(OpeningTypeRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry cannot be null");
	}


	@Override
	public String name() {
		return "definition";
	}
	@Override
	public dev.aperture.pipeline.StageId id() { return dev.aperture.pipeline.StageId.DEFINITION; }

	@Override
	public Class<?> inputType() { return OpeningRequest.class; }

	@Override
	public Class<?> outputType() { return ParameterStage.ResolvedDefinition.class; }

	@Override
	public StageResult<ParameterStage.ResolvedDefinition> execute(OpeningRequest input, StageContext ctx) {
		OpeningRequest request = Objects.requireNonNull(input, "input cannot be null");
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
			new ParameterStage.ResolvedDefinition(typeDefinition, editor.overridesOnly(), request.state())
		);
	}

	private static String normalizeLegacyTypeId(String typeId) {
		return switch (typeId) {
			case "aperture:door_standard" -> "aperture:door";
			case "aperture:window_standard" -> "aperture:fixed_window";
			default -> typeId;
		};
	}

	public record OpeningRequest(
	String typeId,
	Map<String, Object> userParameters,
	dev.aperture.core.instance.OpeningState state
) {
		public OpeningRequest(String typeId, Map<String, Object> userParameters) {
			this(typeId, userParameters, dev.aperture.core.instance.OpeningState.CLOSED);
		}

		public OpeningRequest {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
			Objects.requireNonNull(state, "state cannot be null");
		}

		public static OpeningRequest of(String typeId) {
			return new OpeningRequest(typeId, Map.of(), dev.aperture.core.instance.OpeningState.CLOSED);
		}
	}
}
