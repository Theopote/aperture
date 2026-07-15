package dev.aperture.core.constraint;

/**
 * Parsed constraint expression AST.
 */
public sealed interface ConstraintExpression permits
	ConstraintExpression.NumberLiteral,
	ConstraintExpression.StringLiteral,
	ConstraintExpression.BooleanLiteral,
	ConstraintExpression.ParameterReference,
	ConstraintExpression.Unary,
	ConstraintExpression.Binary,
	ConstraintExpression.Logical {

	record NumberLiteral(double value) implements ConstraintExpression {
	}

	record StringLiteral(String value) implements ConstraintExpression {
	}

	record BooleanLiteral(boolean value) implements ConstraintExpression {
	}

	record ParameterReference(String name) implements ConstraintExpression {
	}

	enum UnaryOperator {
		NEGATE,
		NOT
	}

	record Unary(UnaryOperator operator, ConstraintExpression operand) implements ConstraintExpression {
	}

	enum BinaryOperator {
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		GREATER,
		GREATER_EQUAL,
		LESS,
		LESS_EQUAL,
		EQUAL,
		NOT_EQUAL
	}

	record Binary(BinaryOperator operator, ConstraintExpression left, ConstraintExpression right) implements ConstraintExpression {
	}

	enum LogicalOperator {
		AND,
		OR
	}

	record Logical(LogicalOperator operator, ConstraintExpression left, ConstraintExpression right) implements ConstraintExpression {
	}
}
