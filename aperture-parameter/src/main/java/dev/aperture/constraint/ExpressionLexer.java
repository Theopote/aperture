package dev.aperture.constraint;

import java.util.ArrayList;
import java.util.List;

final class ExpressionLexer {
	private final String input;
	private int index;

	ExpressionLexer(String input) {
		this.input = input;
	}

	List<Token> tokenize() {
		List<Token> tokens = new ArrayList<>();
		while (index < input.length()) {
			skipWhitespace();
			if (index >= input.length()) {
				break;
			}
			char current = input.charAt(index);
			if (Character.isDigit(current) || current == '.') {
				tokens.add(readNumber());
				continue;
			}
			if (Character.isLetter(current) || current == '_') {
				tokens.add(readIdentifier());
				continue;
			}
			if (current == '"') {
				tokens.add(readString());
				continue;
			}
			tokens.add(readOperator());
		}
		tokens.add(new Token(TokenType.EOF, "", index));
		return tokens;
	}

	private void skipWhitespace() {
		while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
			index++;
		}
	}

	private Token readNumber() {
		int start = index;
		while (index < input.length()) {
			char current = input.charAt(index);
			if (Character.isDigit(current) || current == '.') {
				index++;
				continue;
			}
			break;
		}
		return new Token(TokenType.NUMBER, input.substring(start, index), start);
	}

	private Token readIdentifier() {
		int start = index;
		while (index < input.length()) {
			char current = input.charAt(index);
			if (Character.isLetterOrDigit(current) || current == '_') {
				index++;
				continue;
			}
			break;
		}
		String text = input.substring(start, index);
		TokenType type = switch (text) {
			case "true", "false" -> TokenType.BOOLEAN;
			case "and" -> TokenType.AND;
			case "or" -> TokenType.OR;
			case "not" -> TokenType.NOT;
			default -> TokenType.IDENT;
		};
		return new Token(type, text, start);
	}

	private Token readString() {
		index++;
		int start = index;
		StringBuilder builder = new StringBuilder();
		while (index < input.length()) {
			char current = input.charAt(index++);
			if (current == '"') {
				return new Token(TokenType.STRING, builder.toString(), start);
			}
			builder.append(current);
		}
		throw new ConstraintExpressionException("Unterminated string literal at " + start);
	}

	private Token readOperator() {
		int start = index;
		char current = input.charAt(index++);
		switch (current) {
			case '+' -> {
				return new Token(TokenType.PLUS, "+", start);
			}
			case '-' -> {
				return new Token(TokenType.MINUS, "-", start);
			}
			case '*' -> {
				return new Token(TokenType.STAR, "*", start);
			}
			case '/' -> {
				return new Token(TokenType.SLASH, "/", start);
			}
			case '(' -> {
				return new Token(TokenType.LPAREN, "(", start);
			}
			case ')' -> {
				return new Token(TokenType.RPAREN, ")", start);
			}
			case '>' -> {
				if (match('=')) {
					return new Token(TokenType.GTE, ">=", start);
				}
				return new Token(TokenType.GT, ">", start);
			}
			case '<' -> {
				if (match('=')) {
					return new Token(TokenType.LTE, "<=", start);
				}
				return new Token(TokenType.LT, "<", start);
			}
			case '=' -> {
				if (!match('=')) {
					throw new ConstraintExpressionException("Expected == at " + start);
				}
				return new Token(TokenType.EQ, "==", start);
			}
			case '!' -> {
				if (!match('=')) {
					throw new ConstraintExpressionException("Expected != at " + start);
				}
				return new Token(TokenType.NEQ, "!=", start);
			}
			case '&' -> {
				if (!match('&')) {
					throw new ConstraintExpressionException("Expected && at " + start);
				}
				return new Token(TokenType.AND, "&&", start);
			}
			case '|' -> {
				if (!match('|')) {
					throw new ConstraintExpressionException("Expected || at " + start);
				}
				return new Token(TokenType.OR, "||", start);
			}
			default -> throw new ConstraintExpressionException("Unexpected character '" + current + "' at " + start);
		}
	}

	private boolean match(char expected) {
		if (index < input.length() && input.charAt(index) == expected) {
			index++;
			return true;
		}
		return false;
	}

	enum TokenType {
		NUMBER,
		STRING,
		BOOLEAN,
		IDENT,
		PLUS,
		MINUS,
		STAR,
		SLASH,
		GT,
		GTE,
		LT,
		LTE,
		EQ,
		NEQ,
		AND,
		OR,
		NOT,
		LPAREN,
		RPAREN,
		EOF
	}

	record Token(TokenType type, String text, int position) {
	}
}
