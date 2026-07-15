package dev.aperture.render.data;

import dev.aperture.geometry.model.PartId;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Result of comparing two geometry snapshots by stable part identity.
 */
public record RenderDelta(
	Set<PartId> added,
	Set<PartId> removed,
	Set<PartId> changed,
	Set<PartId> unchanged
) {
	public RenderDelta {
		added = copySet(added);
		removed = copySet(removed);
		changed = copySet(changed);
		unchanged = copySet(unchanged);
	}

	public boolean isEmpty() {
		return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
	}

	public Set<PartId> dirty() {
		Set<PartId> dirty = new LinkedHashSet<>(added);
		dirty.addAll(changed);
		return Collections.unmodifiableSet(dirty);
	}

	private static Set<PartId> copySet(Set<PartId> source) {
		return Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(source, "source")));
	}
}
