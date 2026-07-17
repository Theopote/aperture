package dev.aperture.geometry.kinematic;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable path identifying a generated component and all of its dynamic children. */
public record ComponentPath(String value) {
	private static final Pattern PATH = Pattern.compile("[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)*");
	public ComponentPath {
		Objects.requireNonNull(value, "value");
		if (!PATH.matcher(value).matches()) throw new IllegalArgumentException("Invalid component path: " + value);
	}

	public ComponentPath child(String segment) { return new ComponentPath(value + "." + segment); }
	public boolean contains(ComponentPath candidate) {
		return candidate.value.equals(value) || candidate.value.startsWith(value + ".");
	}
	@Override public String toString() { return value; }
}
