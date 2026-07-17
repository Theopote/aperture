package dev.aperture.runtime.model.behavior;

import java.util.Objects;
import java.util.regex.Pattern;

public record BehaviorId(String value) {
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
	public BehaviorId {
		Objects.requireNonNull(value, "value");
		if (!ID.matcher(value).matches()) throw new IllegalArgumentException("Behavior ID must be namespace:path: " + value);
	}
}
