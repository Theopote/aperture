package dev.aperture.runtime.model.capability;

public interface PersistableCapability extends Capability {
	int persistenceSchemaVersion();
}
