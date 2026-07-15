package dev.aperture.render.data;

import dev.aperture.geometry.model.PartId;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Index of render parts keyed by stable part identity.
 */
public final class PartRegistry {
	private final Map<PartId, RenderPart> parts = new LinkedHashMap<>();

	public Collection<RenderPart> all() {
		return Collections.unmodifiableCollection(parts.values());
	}

	public Set<PartId> ids() {
		return Collections.unmodifiableSet(parts.keySet());
	}

	public Optional<RenderPart> get(PartId id) {
		return Optional.ofNullable(parts.get(id));
	}

	public void put(RenderPart part) {
		parts.put(part.id(), part);
	}

	public void remove(PartId id) {
		parts.remove(id);
	}

	public void clear() {
		parts.clear();
	}

	public int size() {
		return parts.size();
	}
}
