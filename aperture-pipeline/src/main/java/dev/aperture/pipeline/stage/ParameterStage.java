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
public final class ParameterStage implements PipelineStage<ResolvedDefinition, ParameterSet> {

	@Override
	public String name() {
		return "parameter";
	}

	@Override
	public StageResult<ParameterSet> execute(ResolvedDefinition input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Resolving parameters for type: " + input.typeDefinition().id());

		try {
			// Resolve sparse overrides against type schema
			ParameterSet resolved = InstanceParameters.resolve(
				input.typeDefinition(),
				input.userParameters()
			);

			ctx.debug("Resolved " + resolved.size() + " parameters");

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
