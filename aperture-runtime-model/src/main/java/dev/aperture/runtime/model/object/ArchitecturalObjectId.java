package dev.aperture.runtime.model.object;

import java.util.Objects;
import java.util.UUID;

/** Stable identity of one architectural object in a world or document. */
public record ArchitecturalObjectId(UUID value) {
	public ArchitecturalObjectId {
		Objects.requireNonNull(value, "value");
	}

	public static ArchitecturalObjectId random() {
		return new ArchitecturalObjectId(UUID.randomUUID());
	}

	public static ArchitecturalObjectId parse(String value) {
		return new ArchitecturalObjectId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
