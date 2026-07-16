package dev.aperture.pipeline.stage;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.InstanceParameters;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Resolves sparse overrides while retaining the definition for later stages. */
public final class ParameterStage implements PipelineStage<Object, ParameterStage.ResolvedParameters> {
	@Override
	public String name() {
		return "parameter";
	}

	@Override
	public StageResult<ResolvedParameters> execute(Object input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		if (!(input instanceof ResolvedDefinition(OpeningTypeDefinition typeDefinition, ParameterSet userParameters))) {
			return new StageResult.Failure<>(
				"ParameterStage requires ResolvedDefinition input but got: " + input.getClass().getSimpleName()
			);
		}
		try {
			ParameterSet parameters = InstanceParameters.resolve(
                    typeDefinition,
                    userParameters
			);
			return new StageResult.Success<>(
				new ResolvedParameters(typeDefinition, parameters)
			);
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to resolve parameters: " + exception.getMessage(), exception);
		}
	}

	public record ResolvedDefinition(
		OpeningTypeDefinition typeDefinition,
		ParameterSet userParameters
	) {
		public ResolvedDefinition {
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
		}
	}

	public record ResolvedParameters(
		OpeningTypeDefinition typeDefinition,
		ParameterSet parameters
	) {
		public ResolvedParameters {
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
			Objects.requireNonNull(parameters, "parameters cannot be null");
		}
	}
}
