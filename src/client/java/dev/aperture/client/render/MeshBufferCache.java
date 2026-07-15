package dev.aperture.client.render;

import dev.aperture.render.mesh.MeshHandle;
import dev.aperture.render.mesh.MeshSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks compiled mesh sections uploaded by the Fabric render backend.
 */
public final class MeshBufferCache {
	private final Map<MeshHandle, MeshSection> sections = new HashMap<>();

	public void track(MeshSection section) {
		sections.put(section.handle(), section);
	}

	public Optional<MeshSection> get(MeshHandle handle) {
		return Optional.ofNullable(sections.get(handle));
	}

	public void release(MeshHandle handle) {
		sections.remove(handle);
	}

	public void clear() {
		sections.clear();
	}
}
