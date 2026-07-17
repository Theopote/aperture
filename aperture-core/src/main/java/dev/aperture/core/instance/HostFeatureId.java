package dev.aperture.core.instance;

import java.util.Objects;

/** Stable identifier of a feature inside a host object. */
public record HostFeatureId(HostFeatureType type, String value) {
	public HostFeatureId {
		Objects.requireNonNull(type, "type");
		if (value == null || value.isBlank()) throw new IllegalArgumentException("Host feature value must not be blank");
	}

	public static HostFeatureId face(String value) { return new HostFeatureId(HostFeatureType.FACE, value); }
	public static HostFeatureId edge(String value) { return new HostFeatureId(HostFeatureType.EDGE, value); }
	public static HostFeatureId namedAnchor(String value) { return new HostFeatureId(HostFeatureType.NAMED_ANCHOR, value); }
}
