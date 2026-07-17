package dev.aperture.runtime.model.capability;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable namespaced and runtime-type-safe capability identity. */
public record CapabilityKey<T extends Capability>(String id, Class<T> type) {
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public CapabilityKey {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(type, "type");
		if (!ID.matcher(id).matches()) throw new IllegalArgumentException("Capability ID must be namespace:path: " + id);
	}

	public static <T extends Capability> CapabilityKey<T> of(String id, Class<T> type) {
		return new CapabilityKey<>(id, type);
	}

	public T cast(Capability capability) {
		return type.cast(capability);
	}
}
