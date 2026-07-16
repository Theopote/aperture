package dev.aperture.pipeline.stage;

import dev.aperture.core.constraint.ExpressionConstraintValidator;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Validates resolved parameters against the definition's constraints. */
public final class ConstraintStage implements PipelineStage<ParameterStage.ResolvedParameters, ConstraintStage.ValidatedParameters> {
	private final ExpressionConstraintValidator validator;

	public ConstraintStage() {
		this(new ExpressionConstraintValidator());
	}

	public ConstraintStage(ExpressionConstraintValidator validator) {
		this.validator = Objects.requireNonNull(validator, "validator cannot be null");
	}

	@Override
	public String name() {
		return "constraint";
	}

	@Override
	public StageResult<ValidatedParameters> execute(ParameterStage.ResolvedParameters input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		var result = validator.validateResolved(input.typeDefinition(), input.parameters());
		if (!result.isValid()) {
			String message = result.issues().stream()
				.map(issue -> issue.message())
				.reduce((left, right) -> left + "; " + right)
				.orElse("Unknown constraint violation");
			return new StageResult.Failure<>("Constraint validation failed: " + message);
		}
		return new StageResult.Success<>(new ValidatedParameters(input.parameters(), input.typeDefinition()));
	}

	public record ValidatedParameters(ParameterSet parameters, OpeningTypeDefinition typeDefinition) {
		public ValidatedParameters {
			Objects.requireNonNull(parameters, "parameters cannot be null");
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
		}
	}
}
