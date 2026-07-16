package dev.aperture.pipeline.stage;

import dev.aperture.constraint.ExpressionConstraintValidator;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;
import dev.aperture.core.validation.ValidationResult;

import java.util.Objects;

/**
 * Constraint validation stage.
 * <p>
 * Validates that the resolved parameters satisfy all constraints
 * defined in the opening type definition.
 * <p>
 * Input: {@link ParameterSet} (resolved parameters)
 * Output: {@link ValidatedParameters} (validated parameters + type definition)
 */
public final class ConstraintStage implements PipelineStage<ParameterSet, ConstraintStage.ValidatedParameters> {

	private final ExpressionConstraintValidator validator;
	private final OpeningTypeDefinition typeDefinition;

	/**
	 * Create constraint stage.
	 *
	 * @param typeDefinition Type definition containing constraints
	 */
	public ConstraintStage(OpeningTypeDefinition typeDefinition) {
		this.typeDefinition = Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
		this.validator = new ExpressionConstraintValidator();
	}

	@Override
	public String name() {
		return "constraint";
	}

	@Override
	public StageResult<ValidatedParameters> execute(ParameterSet input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Validating constraints for " + typeDefinition.id());

		// Validate constraints
		ValidationResult result = validator.validate(typeDefinition, input);

		if (!result.isValid()) {
			// Collect all constraint violations
			String errorMessage = "Constraint validation failed:\n" +
				result.issues().stream()
					.map(issue -> "  - " + issue.message())
					.reduce((a, b) -> a + "\n" + b)
					.orElse("Unknown constraint violation");

			return new StageResult.Failure<>(errorMessage);
		}

		ctx.debug("All constraints satisfied");

		return new StageResult.Success<>(
			new ValidatedParameters(input, typeDefinition)
		);
	}

	/**
	 * Output from ConstraintStage.
	 * Contains validated parameters and type definition for downstream stages.
	 */
	public record ValidatedParameters(
		ParameterSet parameters,
		OpeningTypeDefinition typeDefinition
	) {
		public ValidatedParameters {
			Objects.requireNonNull(parameters, "parameters cannot be null");
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
		}
	}
}
