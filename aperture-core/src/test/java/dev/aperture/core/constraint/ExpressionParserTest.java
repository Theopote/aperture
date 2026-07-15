package dev.aperture.core.constraint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ExpressionParserTest {
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
