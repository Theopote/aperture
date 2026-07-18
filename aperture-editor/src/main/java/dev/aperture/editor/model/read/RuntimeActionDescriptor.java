package dev.aperture.editor.model.read;

/** Capability-derived runtime intent exposed to frontends without leaking mutable capabilities. */
public record RuntimeActionDescriptor(String id, String label, boolean enabled) {
	public RuntimeActionDescriptor {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
		if (label == null || label.isBlank()) throw new IllegalArgumentException("label is required");
	}
}
