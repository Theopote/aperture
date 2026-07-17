package dev.aperture.runtime.model.object;

import dev.aperture.runtime.model.capability.CapabilityKey;
import dev.aperture.runtime.model.capability.CapabilityProvider;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.capability.Capability;

import java.util.Optional;

/** Activated execution view of a durable architectural object instance. */
public interface RuntimeArchitecturalObject extends CapabilityProvider {
	ArchitecturalObjectDefinition definition();

	ArchitecturalObjectInstance instance();

	CapabilitySet capabilities();

	@Override
	default <T extends Capability> Optional<T> capability(CapabilityKey<T> key) {
		return capabilities().capability(key);
	}

	default ArchitecturalObjectId objectId() {
		return instance().objectId();
	}

	default ArchitecturalTypeId typeId() {
		return instance().typeId();
	}
}
