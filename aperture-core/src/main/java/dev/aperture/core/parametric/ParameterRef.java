package dev.aperture.core.parametric;

import java.util.Objects;

/**
 * Typed reference from a component recipe field to an opening type parameter.
 * Serialized in data packs as {@code "parameter:<name>"}.
 */
public record ParameterRef(String name) {
	public ParameterRef {
		Objects.requireNonNull(name, "name");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
	}

	public static final String PREFIX = "parameter:";

	public static ParameterRef of(String name) {
		return new ParameterRef(name);
	}

	public static ParameterRef parse(String source) {
		Objects.requireNonNull(source, "source");
		if (!source.startsWith(PREFIX)) {
			throw new IllegalArgumentException("Expected parameter reference, got: " + source);
		}
		return new ParameterRef(source.substring(PREFIX.length()));
	}

	public static boolean isReference(String source) {
		return source != null && source.startsWith(PREFIX);
	}

	public String sourceExpression() {
		return PREFIX + name;
	}
}
