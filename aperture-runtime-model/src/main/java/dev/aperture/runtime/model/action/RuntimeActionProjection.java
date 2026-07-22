package dev.aperture.runtime.model.action;

/** Family-owned, immutable runtime action projected for clients and tools. */
public record RuntimeActionProjection(String id, String label, boolean enabled) {
	public RuntimeActionProjection {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
		if (label == null || label.isBlank()) throw new IllegalArgumentException("label is required");
	}
}
