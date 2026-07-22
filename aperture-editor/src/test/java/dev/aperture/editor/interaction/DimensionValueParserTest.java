package dev.aperture.editor.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionValueParserTest {
	private final DimensionValueParser parser = new DimensionValueParser();

	@Test void parsesAbsoluteMillimetres() { assertEquals(1350, parser.parse("1350", 900).millimeters()); }
	@Test void parsesMetres() { assertEquals(1350, parser.parse("1.35m", 900).millimeters()); }
	@Test void parsesPositiveRelativeValue() { assertEquals(1000, parser.parse("+100", 900).millimeters()); }
	@Test void parsesNegativeRelativeValue() { assertEquals(850, parser.parse("-50", 900).millimeters()); }
	@Test void rejectsInvalidAndNegativeResults() {
		assertThrows(IllegalArgumentException.class, () -> parser.parse("door", 900));
		assertThrows(IllegalArgumentException.class, () -> parser.parse("-1000", 900));
	}
}
