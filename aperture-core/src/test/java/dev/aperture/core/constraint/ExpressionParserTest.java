package dev.aperture.core.constraint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionParserTest {
	@Test
	void tokenizesMultiplyExpression() {
		var tokens = new ExpressionLexer("height * 2").tokenize();
		assertEquals(ExpressionLexer.TokenType.IDENT, tokens.get(0).type());
		assertEquals(ExpressionLexer.TokenType.STAR, tokens.get(1).type());
		assertEquals(ExpressionLexer.TokenType.NUMBER, tokens.get(2).type());
		assertEquals(ExpressionLexer.TokenType.EOF, tokens.get(3).type());
	}

	@Test
	void parsesIdentifierAlone() {
		assertDoesNotThrow(() -> new ExpressionParser().parse("width"));
	}
	@Test
	void parsesMultiplicationAlone() {
		assertDoesNotThrow(() -> new ExpressionParser().parse("height * 2"));
	}

	@Test
	void parsesComparisonAlone() {
		assertDoesNotThrow(() -> new ExpressionParser().parse("width > height"));
	}

	@Test
	void printsTokensForDebugging() {
		var tokens = new ExpressionLexer("width > height * 0.5").tokenize();
		for (ExpressionLexer.Token token : tokens) {
			System.out.println(token.type() + " '" + token.text() + "' @" + token.position());
		}
	}

	@Test
	void parsesComparisonWithMultiplication() {
		assertDoesNotThrow(() -> new ExpressionParser().parse("width > height * 0.5"));
	}

	@Test
	void parsesComparisonWithIntegerMultiplication() {
		assertDoesNotThrow(() -> new ExpressionParser().parse("width > height * 2"));
	}
}
