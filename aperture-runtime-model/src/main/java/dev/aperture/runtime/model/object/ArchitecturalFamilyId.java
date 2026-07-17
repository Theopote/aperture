package dev.aperture.runtime.model.object;

import java.util.Objects;
import java.util.regex.Pattern;

/** Family discriminator such as aperture:opening; it is not a Java class hierarchy. */
public record ArchitecturalFamilyId(String value) {
	private static final Pattern VALUE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public ArchitecturalFamilyId {
		Objects.requireNonNull(value, "value");
		if (!VALUE.matcher(value).matches()) {
			throw new IllegalArgumentException("Family ID must be namespace:path: " + value);
		}
	}
}
