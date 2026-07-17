package dev.aperture.runtime.pipeline;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.state.RuntimeStateStore;

import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts the existing opening store to the family-neutral runtime repository port. */
public final class OpeningInstanceRepository implements RuntimeStateStore {
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

	@Override
	public Optional<ArchitecturalObject> find(UUID objectId) {
		return store.findById(objectId).map(object -> object);
	}

	@Override
	public Collection<ArchitecturalObject> all() {
		return List.copyOf(store.all());
	}
}
