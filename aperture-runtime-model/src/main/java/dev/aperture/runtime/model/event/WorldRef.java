package dev.aperture.runtime.model.event;

/** Platform-neutral world or document identity. */
public record WorldRef(String id) {
	public WorldRef { id = References.requireNamespaced(id, "world"); }
}
