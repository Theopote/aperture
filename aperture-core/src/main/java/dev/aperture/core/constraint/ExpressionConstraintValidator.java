package dev.aperture.core.constraint;

import dev.aperture.core.definition.ConstraintRule;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ValidationIssue;
import dev.aperture.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates declarative {@link ConstraintRule} expressions against resolved parameters.
 */
public final class ExpressionConstraintValidator implements OpeningValidator {
	private final ConstraintEvaluator evaluator = new ConstraintEvaluator();

	@Override
	public ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet resolved = definition.resolveParameters(instance.parameters());
		return validateResolved(definition, resolved);
	}

	public ValidationResult validateResolved(OpeningTypeDefinition definition, ParameterSet resolved) {
		if (definition.constraints().isEmpty()) {
			return ValidationResult.OK;
		}

		ConstraintContext context = new ConstraintContext(definition.parametricSchema(), resolved);
		List<ValidationIssue> issues = new ArrayList<>();

		for (ConstraintRule rule : definition.constraints()) {
			try {
				if (!evaluator.evaluate(rule.expression(), context)) {
					issues.add(ValidationIssue.error("constraint.failed", rule.message()));
				}
			} catch (ConstraintExpressionException exception) {
				issues.add(ValidationIssue.error(
					"constraint.invalid_expression",
					rule.expression() + ": " + exception.getMessage()
				));
			}
		}

		return new ValidationResult(issues);
	}
}
