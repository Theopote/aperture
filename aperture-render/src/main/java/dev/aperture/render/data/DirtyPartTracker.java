package dev.aperture.render.data;

import dev.aperture.geometry.model.PartId;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks parts awaiting mesh compilation. Coalesces rapid edits into one dirty set per frame.
 */
public final class DirtyPartTracker {
	private final Set<PartId> pending = new LinkedHashSet<>();

	public void mark(PartId partId) {
		pending.add(partId);
	}

	public void markAll(Set<PartId> partIds) {
		pending.addAll(partIds);
	}

	public Set<PartId> drain() {
		if (pending.isEmpty()) {
			return Set.of();
		}
		Set<PartId> snapshot = Set.copyOf(pending);
		pending.clear();
		return snapshot;
	}

	public Set<PartId> pending() {
		return Collections.unmodifiableSet(pending);
	}

	public boolean isEmpty() {
		return pending.isEmpty();
	}
}
