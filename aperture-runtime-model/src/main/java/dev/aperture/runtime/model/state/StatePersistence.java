package dev.aperture.runtime.model.state;

/** Determines whether a property is durable, runtime-only, or computed. */
public enum StatePersistence {
	PERSISTENT,
	TRANSIENT,
	DERIVED
}
