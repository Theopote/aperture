package dev.aperture.runtime.model.persistence;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic persistence adapter for Minecraft-free runtime tests. */
public final class InMemoryArchitecturalObjectPersistence implements ArchitecturalObjectPersistence {
	private final Map<ArchitecturalObjectId, ArchitecturalObjectSnapshot> snapshots = new LinkedHashMap<>();

	@Override public synchronized void save(ArchitecturalObjectSnapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot");
		snapshots.put(snapshot.instance().objectId(), snapshot);
	}

	@Override public synchronized Optional<ArchitecturalObjectSnapshot> load(ArchitecturalObjectId objectId) {
		return Optional.ofNullable(snapshots.get(objectId));
	}

	@Override public synchronized void delete(ArchitecturalObjectId objectId) { snapshots.remove(objectId); }
}
