package dev.aperture.core.constraint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates parsed constraint expressions against resolved parameter values.
 */
public final class ConstraintEvaluator {
	private static final Map<String, ConstraintExpression> CACHE = new ConcurrentHashMap<>();
	private static final ExpressionParser PARSER = new ExpressionParser();

	public boolean evaluate(String expression, ConstraintContext context) {
		ConstraintExpression parsed = CACHE.computeIfAbsent(expression, PARSER::parse);
		return evaluate(parsed, context);
	}

	public boolean evaluate(ConstraintExpression expression, ConstraintContext context) {
		return switch (expression) {
			case ConstraintExpression.NumberLiteral literal -> literal.value() != 0.0;
			case ConstraintExpression.StringLiteral literal -> !literal.value().isEmpty();
			case ConstraintExpression.BooleanLiteral literal -> literal.value();
			case ConstraintExpression.ParameterReference reference -> evaluateParameter(reference, context);
			case ConstraintExpression.Unary unary -> evaluateUnary(unary, context);
			case ConstraintExpression.Binary binary -> evaluateBinary(binary, context);
			case ConstraintExpression.Logical logical -> evaluateLogical(logical, context);
		};
	}

	private static boolean evaluateParameter(ConstraintExpression.ParameterReference reference, ConstraintContext context) {
		try {
			return context.numeric(reference.name()) != 0.0;
		} catch (IllegalArgumentException notNumeric) {
			return !context.text(reference.name()).isEmpty();
		}
	}

	private boolean evaluateUnary(ConstraintExpression.Unary unary, ConstraintContext context) {
		return switch (unary.operator()) {
			case NEGATE -> evaluateNumber(unary, context) != 0.0;
			case NOT -> !evaluate(unary.operand(), context);
		};
	}

	private boolean evaluateBinary(ConstraintExpression.Binary binary, ConstraintContext context) {
		return switch (binary.operator()) {
			case EQUAL -> equalsValue(binary.left(), binary.right(), context);
			case NOT_EQUAL -> !equalsValue(binary.left(), binary.right(), context);
			case GREATER -> compareNumbers(binary.left(), binary.right(), context) > 0;
			case GREATER_EQUAL -> compareNumbers(binary.left(), binary.right(), context) >= 0;
			case LESS -> compareNumbers(binary.left(), binary.right(), context) < 0;
			case LESS_EQUAL -> compareNumbers(binary.left(), binary.right(), context) <= 0;
			case ADD, SUBTRACT, MULTIPLY, DIVIDE -> evaluateNumber(binary, context) != 0.0;
		};
	}

	private boolean evaluateLogical(ConstraintExpression.Logical logical, ConstraintContext context) {
		return switch (logical.operator()) {
			case AND -> evaluate(logical.left(), context) && evaluate(logical.right(), context);
			case OR -> evaluate(logical.left(), context) || evaluate(logical.right(), context);
		};
	}

	private double evaluateNumber(ConstraintExpression expression, ConstraintContext context) {
		return switch (expression) {
			case ConstraintExpression.NumberLiteral literal -> literal.value();
			case ConstraintExpression.BooleanLiteral literal -> literal.value() ? 1.0 : 0.0;
			case ConstraintExpression.ParameterReference reference -> context.numeric(reference.name());
			case ConstraintExpression.Unary unary when unary.operator() == ConstraintExpression.UnaryOperator.NEGATE ->
				-evaluateNumber(unary.operand(), context);
			case ConstraintExpression.Binary binary -> evaluateNumber(binary, context);
			default -> throw new ConstraintExpressionException("Expected numeric expression");
		};
	}

	private double evaluateNumber(ConstraintExpression.Binary binary, ConstraintContext context) {
		double left = evaluateNumber(binary.left(), context);
		double right = evaluateNumber(binary.right(), context);
		return switch (binary.operator()) {
			case ADD -> left + right;
			case SUBTRACT -> left - right;
			case MULTIPLY -> left * right;
			case DIVIDE -> {
				if (right == 0.0) {
					throw new ConstraintExpressionException("Division by zero");
				}
				yield left / right;
			}
			default -> throw new ConstraintExpressionException("Expected arithmetic operator");
		};
	}

	private int compareNumbers(
		ConstraintExpression left,
		ConstraintExpression right,
		ConstraintContext context
	) {
		return Double.compare(evaluateNumber(left, context), evaluateNumber(right, context));
	}

	private boolean equalsValue(
		ConstraintExpression left,
		ConstraintExpression right,
		ConstraintContext context
	) {
		if (left instanceof ConstraintExpression.StringLiteral leftString) {
			return leftString.value().equals(stringValue(right, context));
		}
		if (right instanceof ConstraintExpression.StringLiteral rightString) {
			return rightString.value().equals(stringValue(left, context));
		}
		if (left instanceof ConstraintExpression.BooleanLiteral || right instanceof ConstraintExpression.BooleanLiteral) {
			return evaluate(left, context) == evaluate(right, context);
		}
		return evaluateNumber(left, context) == evaluateNumber(right, context);
	}

	private String stringValue(ConstraintExpression expression, ConstraintContext context) {
		return switch (expression) {
			case ConstraintExpression.StringLiteral literal -> literal.value();
			case ConstraintExpression.ParameterReference reference -> context.text(reference.name());
			default -> throw new ConstraintExpressionException("Expected string expression");
		};
	}
}
