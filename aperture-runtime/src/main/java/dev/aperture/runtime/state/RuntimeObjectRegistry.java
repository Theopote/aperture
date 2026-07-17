package dev.aperture.runtime.state;

import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.pipeline.RuntimeObjectRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Authoritative live-object registry backed by a persistence-facing state store. */
public final class RuntimeObjectRegistry implements RuntimeObjectRepository {
	private final ConcurrentMap<UUID, ArchitecturalObject> liveObjects = new ConcurrentHashMap<>();
	private final RuntimeStateStore stateStore;

	public RuntimeObjectRegistry(RuntimeStateStore stateStore) {
		this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
	}

	public ArchitecturalObject register(ArchitecturalObject object) {
		Objects.requireNonNull(object, "object");
		liveObjects.compute(object.instanceId(), (id, existing) -> {
			if (existing != null && !existing.equals(object)) {
				throw new IllegalStateException("Runtime object already registered: " + id);
			}
			if (existing == null) {
				stateStore.save(object);
			}
			return object;
		});
		return object;
	}

	public Optional<ArchitecturalObject> find(UUID objectId) {
		Objects.requireNonNull(objectId, "objectId");
		ArchitecturalObject live = liveObjects.get(objectId);
		if (live != null) {
			return Optional.of(live);
		}
		Optional<ArchitecturalObject> loaded = stateStore.find(objectId);
		loaded.ifPresent(object -> liveObjects.putIfAbsent(objectId, object));
		return loaded;
	}

	public ArchitecturalObject require(UUID objectId) {
		return find(objectId).orElseThrow(() -> new IllegalArgumentException("Unknown runtime object: " + objectId));
	}

	public Collection<ArchitecturalObject> snapshot() {
		return List.copyOf(liveObjects.values());
	}
	public boolean unregister(UUID objectId) {
		Objects.requireNonNull(objectId, "objectId");
		ArchitecturalObject removed = liveObjects.remove(objectId);
		return stateStore.remove(objectId) || removed != null;
	}


	@Override
	public void save(ArchitecturalObject object) {
		Objects.requireNonNull(object, "object");
		liveObjects.compute(object.instanceId(), (id, current) -> {
			if (current != null && object.revision() < current.revision()) {
				throw new IllegalStateException("Runtime revision moved backwards for " + id);
			}
			if (current != null && object.revision() == current.revision() && !object.equals(current)) {
				throw new IllegalStateException("Conflicting runtime snapshots at revision " + object.revision());
			}
			stateStore.save(object);
			return object;
		});
	}
}
