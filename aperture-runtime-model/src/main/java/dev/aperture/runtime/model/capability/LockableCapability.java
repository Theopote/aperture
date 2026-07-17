package dev.aperture.runtime.model.capability;

/** Observable lock semantics; lock mutation is performed through Commands. */
public interface LockableCapability extends Capability {
	boolean locked();
	boolean canChangeLock();
}
