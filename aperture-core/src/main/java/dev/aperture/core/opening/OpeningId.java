package dev.aperture.core.opening;

import java.util.Objects;

/**
 * Namespaced identifier for opening types, generators, profiles, etc.
 * Format: {@code namespace:path} e.g. {@code aperture:fixed_window}.
 */
public record OpeningId(String namespace, String path) {
	public static final String APERTURE_NAMESPACE = "aperture";

	public OpeningId {
		Objects.requireNonNull(namespace, "namespace");
		Objects.requireNonNull(path, "path");
		if (namespace.isBlank() || path.isBlank()) {
			throw new IllegalArgumentException("OpeningId parts must not be blank");
		}
	}

	public static OpeningId parse(String raw) {
		int separator = raw.indexOf(':');
		if (separator <= 0 || separator == raw.length() - 1) {
			throw new IllegalArgumentException("Invalid OpeningId: " + raw);
		}
		return new OpeningId(raw.substring(0, separator), raw.substring(separator + 1));
	}

	public static OpeningId aperture(String path) {
		return new OpeningId(APERTURE_NAMESPACE, path);
	}

	@Override
	public String toString() {
		return namespace + ":" + path;
	}
}
