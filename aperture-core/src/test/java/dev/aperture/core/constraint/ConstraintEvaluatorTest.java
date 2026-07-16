package dev.aperture.core.constraint;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintEvaluatorTest {
	private final ConstraintEvaluator evaluator = new ConstraintEvaluator();

	@Test
	void evaluatesWidthGreaterThanHalfHeight() {
		var context = context(ParameterSet.builder()
			.put("width", ParameterValue.length(1200))
			.put("height", ParameterValue.length(1500))
			.build());

		assertTrue(evaluator.evaluate("width > height * 0.5", context));
	}

	@Test
	void rejectsWidthBelowHalfHeight() {
		var context = context(ParameterSet.builder()
			.put("width", ParameterValue.length(600))
			.put("height", ParameterValue.length(1500))
			.build());

		assertFalse(evaluator.evaluate("width > height * 0.5", context));
	}

	@Test
	void supportsLogicalAndStringEquality() {
		var context = context(ParameterSet.builder()
			.put("hinge_side", ParameterValue.enumValue("left"))
			.put("has_transom", ParameterValue.bool(false))
			.build());

		assertTrue(evaluator.evaluate("hinge_side == \"left\"", context));
		assertTrue(evaluator.evaluate("not has_transom", context));
		assertTrue(evaluator.evaluate("hinge_side == \"left\" and not has_transom", context));
	}

	private static ConstraintContext context(ParameterSet resolved) {
		var schema = dev.aperture.core.catalog.BuiltinOpeningTypes.door().parametricSchema();
		return new ConstraintContext(schema, schema.mergeDefaults(resolved));
	}
}
