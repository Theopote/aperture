package dev.aperture.runtime.pipeline;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.object.ArchitecturalObject;

import java.util.Objects;

/** Adapts the existing opening store to the family-neutral runtime repository port. */
public final class OpeningInstanceRepository implements RuntimeObjectRepository {
	private final OpeningInstanceStore store;

	public OpeningInstanceRepository(OpeningInstanceStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	@Override
	public void save(ArchitecturalObject object) {
		if (!(object instanceof OpeningInstance opening)) {
			throw new IllegalArgumentException("Opening repository cannot save " + object.getClass().getName());
		}
		store.put(opening);
	}
}
