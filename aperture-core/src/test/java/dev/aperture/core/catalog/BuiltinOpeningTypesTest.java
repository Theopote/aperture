package dev.aperture.core.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinOpeningTypesTest {
	@Test
	void exposesOnlyThreeReferenceTypes() {
		assertEquals(3, BuiltinOpeningTypes.referenceIds().size());
		assertEquals(3, BuiltinOpeningTypes.referenceDefinitions().size());
	}

	@Test
	void referenceIdsMatchDefinitions() {
		for (OpeningTypeDefinition definition : BuiltinOpeningTypes.referenceDefinitions()) {
			assertTrue(BuiltinOpeningTypes.isReferenceType(definition.id()));
		}
	}

	@Test
	void nonReferenceIdsAreRejected() {
		assertFalse(BuiltinOpeningTypes.isReferenceType(OpeningTestFixtures.casementTestId()));
	}
}
