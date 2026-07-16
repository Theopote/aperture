package dev.aperture.constraint;

import java.util.List;

/**
 * Parses constraint expressions such as {@code width > height * 0.5}.
 */
public final class ExpressionParser {
	public ConstraintExpression parse(String expression) {
		List<ExpressionLexer.Token> tokens = new ExpressionLexer(expression).tokenize();
		Parser parser = new Parser(tokens);
		ConstraintExpression parsed = parser.parseExpression();
		parser.expect(ExpressionLexer.TokenType.EOF);
		return parsed;
	}

	private static final class Parser {
		private final List<ExpressionLexer.Token> tokens;
		private int index;

		private Parser(List<ExpressionLexer.Token> tokens) {
			this.tokens = tokens;
		}

		private ConstraintExpression parseExpression() {
			return parseOr();
		}

		private ConstraintExpression parseOr() {
			ConstraintExpression left = parseAnd();
			while (match(ExpressionLexer.TokenType.OR)) {
				left = new ConstraintExpression.Logical(
					ConstraintExpression.LogicalOperator.OR,
					left,
					parseAnd()
				);
			}
			return left;
		}

		private ConstraintExpression parseAnd() {
			ConstraintExpression left = parseEquality();
			while (match(ExpressionLexer.TokenType.AND)) {
				left = new ConstraintExpression.Logical(
					ConstraintExpression.LogicalOperator.AND,
					left,
					parseEquality()
				);
			}
			return left;
		}

		private ConstraintExpression parseEquality() {
			ConstraintExpression left = parseComparison();
			while (true) {
				if (match(ExpressionLexer.TokenType.EQ)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.EQUAL,
						left,
						parseComparison()
					);
				} else if (match(ExpressionLexer.TokenType.NEQ)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.NOT_EQUAL,
						left,
						parseComparison()
					);
				} else {
					return left;
				}
			}
		}

		private ConstraintExpression parseComparison() {
			ConstraintExpression left = parseAdditive();
			while (true) {
				if (match(ExpressionLexer.TokenType.GT)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.GREATER,
						left,
						parseAdditive()
					);
				} else if (match(ExpressionLexer.TokenType.GTE)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.GREATER_EQUAL,
						left,
						parseAdditive()
					);
				} else if (match(ExpressionLexer.TokenType.LT)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.LESS,
						left,
						parseAdditive()
					);
				} else if (match(ExpressionLexer.TokenType.LTE)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.LESS_EQUAL,
						left,
						parseAdditive()
					);
				} else {
					return left;
				}
			}
		}

		private ConstraintExpression parseAdditive() {
			ConstraintExpression left = parseMultiplicative();
			while (true) {
				if (match(ExpressionLexer.TokenType.PLUS)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.ADD,
						left,
						parseMultiplicative()
					);
				} else if (match(ExpressionLexer.TokenType.MINUS)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.SUBTRACT,
						left,
						parseMultiplicative()
					);
				} else {
					return left;
				}
			}
		}

		private ConstraintExpression parseMultiplicative() {
			ConstraintExpression left = parseUnary();
			while (true) {
				if (match(ExpressionLexer.TokenType.STAR)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.MULTIPLY,
						left,
						parseUnary()
					);
				} else if (match(ExpressionLexer.TokenType.SLASH)) {
					left = new ConstraintExpression.Binary(
						ConstraintExpression.BinaryOperator.DIVIDE,
						left,
						parseUnary()
					);
				} else {
					return left;
				}
			}
		}

		private ConstraintExpression parseUnary() {
			if (match(ExpressionLexer.TokenType.MINUS)) {
				return new ConstraintExpression.Unary(
					ConstraintExpression.UnaryOperator.NEGATE,
					parseUnary()
				);
			}
			if (match(ExpressionLexer.TokenType.NOT)) {
				return new ConstraintExpression.Unary(
					ConstraintExpression.UnaryOperator.NOT,
					parseUnary()
				);
			}
			return parsePrimary();
		}

		private ConstraintExpression parsePrimary() {
			if (match(ExpressionLexer.TokenType.NUMBER)) {
				return new ConstraintExpression.NumberLiteral(
					Double.parseDouble(previous().text())
				);
			}
			if (match(ExpressionLexer.TokenType.STRING)) {
				return new ConstraintExpression.StringLiteral(previous().text());
			}
			if (match(ExpressionLexer.TokenType.BOOLEAN)) {
				return new ConstraintExpression.BooleanLiteral(Boolean.parseBoolean(previous().text()));
			}
			if (match(ExpressionLexer.TokenType.IDENT)) {
				return new ConstraintExpression.ParameterReference(previous().text());
			}
			if (match(ExpressionLexer.TokenType.LPAREN)) {
				ConstraintExpression expression = parseExpression();
				expect(ExpressionLexer.TokenType.RPAREN);
				return expression;
			}
			throw error("Expected expression");
		}

		private boolean match(ExpressionLexer.TokenType type) {
			if (check(type)) {
				index++;
				return true;
			}
			return false;
		}

		private void expect(ExpressionLexer.TokenType type) {
			if (!match(type)) {
				throw error("Expected " + type);
			}
		}

		private boolean check(ExpressionLexer.TokenType type) {
			return peek().type() == type;
		}

		private ExpressionLexer.Token peek() {
			return tokens.get(index);
		}

		private ExpressionLexer.Token previous() {
			return tokens.get(index - 1);
		}

		private ConstraintExpressionException error(String message) {
			ExpressionLexer.Token token = peek();
			return new ConstraintExpressionException(message + " at " + token.position());
		}
	}
}
