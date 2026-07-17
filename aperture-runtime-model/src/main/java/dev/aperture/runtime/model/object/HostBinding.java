package dev.aperture.runtime.model.object;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;

import java.util.Objects;

/** Platform-neutral dependency on a feature of another architectural object. */
public record HostBinding(
	ArchitecturalObjectId hostId,
	String featureId,
	Transform3d insertionFrame,
	String attachmentMode,
	ParameterSet attachmentParameters,
	long hostRevision
) {
	public HostBinding {
		Objects.requireNonNull(hostId, "hostId");
		featureId = requireText(featureId, "featureId");
		Objects.requireNonNull(insertionFrame, "insertionFrame");
		attachmentMode = requireText(attachmentMode, "attachmentMode");
		Objects.requireNonNull(attachmentParameters, "attachmentParameters");
		if (hostRevision < 0) throw new IllegalArgumentException("hostRevision must be non-negative");
	}

	private static String requireText(String value, String label) {
		Objects.requireNonNull(value, label);
		if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
		return value;
	}
}
