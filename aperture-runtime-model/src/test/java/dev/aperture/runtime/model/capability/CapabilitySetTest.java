package dev.aperture.runtime.model.capability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilitySetTest {
	private static final OpenableCapability OPENABLE = new OpenableCapability() {
		@Override public double currentRatio() { return 0.25; }
		@Override public double targetRatio() { return 1.0; }
		@Override public boolean canRequestOpen() { return true; }
		@Override public boolean canRequestClose() { return true; }
	};

	@Test
	void resolvesCapabilitiesByStableTypedKey() {
		CapabilitySet capabilities = CapabilitySet.builder()
			.add(StandardCapabilities.OPENABLE, OPENABLE)
			.build();

		assertTrue(capabilities.hasCapability(StandardCapabilities.OPENABLE));
		assertFalse(capabilities.hasCapability(StandardCapabilities.LOCKABLE));
		assertEquals(0.25, capabilities.requireCapability(StandardCapabilities.OPENABLE).currentRatio());
		assertThrows(MissingCapabilityException.class,
			() -> capabilities.requireCapability(StandardCapabilities.LOCKABLE));
	}

	@Test
	void rejectsDuplicateAndMalformedKeys() {
		assertThrows(IllegalArgumentException.class,
			() -> CapabilityKey.of("openable", OpenableCapability.class));
		CapabilitySet.Builder builder = CapabilitySet.builder().add(StandardCapabilities.OPENABLE, OPENABLE);
		assertThrows(IllegalArgumentException.class,
			() -> builder.add(StandardCapabilities.OPENABLE, OPENABLE));
	}
}
