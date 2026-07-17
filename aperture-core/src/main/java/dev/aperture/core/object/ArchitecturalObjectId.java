package dev.aperture.core.object;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Stable identity of an architectural object, independent of its family. */
public record ArchitecturalObjectId(UUID value) {
	public static final ArchitecturalObjectId NONE = new ArchitecturalObjectId(new UUID(0L, 0L));

	public ArchitecturalObjectId {
		Objects.requireNonNull(value, "value");
	}

	public static ArchitecturalObjectId parse(String value) {
		return new ArchitecturalObjectId(UUID.fromString(value));
	}

	public static ArchitecturalObjectId deterministic(String stableKey) {
		Objects.requireNonNull(stableKey, "stableKey");
		return new ArchitecturalObjectId(UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)));
	}

	public boolean isNone() {
		return value.getMostSignificantBits() == 0L && value.getLeastSignificantBits() == 0L;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
