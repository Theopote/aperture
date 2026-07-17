package dev.aperture.runtime.model.event;

import java.util.Objects;
import java.util.regex.Pattern;

final class References {
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+");
	private References() { }

	static String requireNamespaced(String value, String label) {
		Objects.requireNonNull(value, label);
		if (!ID.matcher(value).matches()) throw new IllegalArgumentException(label + " reference must be namespace:path: " + value);
		return value;
	}
}
