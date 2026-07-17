package dev.aperture.runtime.model.object;

import java.util.Objects;
import java.util.regex.Pattern;

/** Namespaced identity of a versioned architectural object type. */
public record ArchitecturalTypeId(String namespace, String path) {
	private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");
	private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

	public ArchitecturalTypeId {
		namespace = requirePart(namespace, PART, "namespace");
		path = requirePart(path, PATH, "path");
	}

	public static ArchitecturalTypeId parse(String value) {
		Objects.requireNonNull(value, "value");
		int separator = value.indexOf(':');
		if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
			throw new IllegalArgumentException("Architectural type ID must be namespace:path: " + value);
		}
		return new ArchitecturalTypeId(value.substring(0, separator), value.substring(separator + 1));
	}

	private static String requirePart(String value, Pattern pattern, String label) {
		Objects.requireNonNull(value, label);
		if (!pattern.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid architectural type " + label + ": " + value);
		}
		return value;
	}

	@Override
	public String toString() {
		return namespace + ':' + path;
	}
}
