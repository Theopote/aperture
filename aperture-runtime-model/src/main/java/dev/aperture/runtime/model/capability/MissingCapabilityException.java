package dev.aperture.runtime.model.capability;

public final class MissingCapabilityException extends IllegalStateException {
	public MissingCapabilityException(String capabilityId) {
		super("Required capability is not available: " + capabilityId);
	}
}
