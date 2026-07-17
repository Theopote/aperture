package dev.aperture.core.object;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.opening.OpeningId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ArchitecturalObjectTest {
	@Test
	void openingInstanceIsTheFirstArchitecturalObjectFamily() {
		OpeningInstance opening = OpeningInstance.builder(OpeningId.aperture("fixed_window")).build();

		ArchitecturalObject object = assertInstanceOf(ArchitecturalObject.class, opening);
		assertEquals(opening.instanceId(), object.instanceId());
		assertEquals(opening.transform(), object.transform());
		assertEquals(opening.parameters(), object.parameters());
		assertEquals(opening.revision(), object.revision());
	}
}
