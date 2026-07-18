package dev.aperture.runtime.model.command;

import dev.aperture.parameter.ParameterValue;

import java.util.Objects;

/** Immutable parameter mutation intent applied only by the authoritative runtime transaction. */
public record ParameterPatch(long expectedObjectRevision, String parameter, ParameterValue value) {
	public ParameterPatch {
		if (expectedObjectRevision < 0) throw new IllegalArgumentException("expectedObjectRevision must be non-negative");
		if (parameter == null || parameter.isBlank()) throw new IllegalArgumentException("parameter must not be blank");
		Objects.requireNonNull(value, "value");
	}
}
