package dev.aperture.core.instance;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryOpeningInstanceStoreTest {
	@Test
	void storesAndRetrievesInstances() {
		InMemoryOpeningInstanceStore store = new InMemoryOpeningInstanceStore();
		UUID id = UUID.randomUUID();
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID)
			.instanceId(id)
			.parameters(ParameterSet.empty())
			.build();

		store.put(instance);

		assertTrue(store.findById(id).isPresent());
		assertEquals(1, store.all().size());
		assertTrue(store.remove(id));
		assertTrue(store.findById(id).isEmpty());
	}
}
