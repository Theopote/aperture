package dev.aperture.core.parametric;

import java.util.Objects;

public record ChoiceOption(String value, String label) {
	public ChoiceOption {
		Objects.requireNonNull(value, "value");
		Objects.requireNonNull(label, "label");
	}
}
