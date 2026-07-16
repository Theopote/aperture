package dev.aperture.pipeline.stage;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.InstanceParameters;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Parameter resolution stage.
 * <p>
 * Resolves sparse parameter overrides against the opening type schema
 * to produce a complete {@link ParameterSet}.
 * <p>
 * Input: {@link ResolvedDefinition} (type definition + user parameters)
 * Output: {@link ParameterSet} (complete parameter values)
 */
public final class ParameterStage implements PipelineStage<Object, ParameterSet> {

	@Override
	public String name() {
		return "parameter";
	}

	@Override
	public StageResult<ParameterSet> execute(Object input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		if (!(input instanceof ResolvedDefinition resolvedDefinition)) {
			return new StageResult.Failure<>(
				"ParameterStage requires ResolvedDefinition input but got: " + input.getClass().getSimpleName()
			);
		}

		ctx.debug("Resolving parameters for type: " + resolvedDefinition.typeDefinition().id());

		try {
			// Resolve sparse overrides against type schema
			ParameterSet resolved = InstanceParameters.resolve(
				resolvedDefinition.typeDefinition(),
				resolvedDefinition.userParameters()
			);

			ctx.debug("Resolved " + resolved.asMap().size() + " parameters");

			return new StageResult.Success<>(resolved);

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to resolve parameters: " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Input for ParameterStage.
	 * Contains type definition and user-provided parameter overrides.
	 */
	public record ResolvedDefinition(
		OpeningTypeDefinition typeDefinition,
		ParameterSet userParameters
	) {
		public ResolvedDefinition {
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
			Objects.requireNonNull(userParameters, "userParameters cannot be null");
		}
	}
}
