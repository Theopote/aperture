package dev.aperture.runtime.model.capability;

import java.util.Optional;

public interface CapabilityProvider {
	<T extends Capability> Optional<T> capability(CapabilityKey<T> key);

	default <T extends Capability> T requireCapability(CapabilityKey<T> key) {
		return capability(key).orElseThrow(() -> new MissingCapabilityException(key.id()));
	}

	default boolean hasCapability(CapabilityKey<?> key) {
		return capability(key).isPresent();
	}
}
