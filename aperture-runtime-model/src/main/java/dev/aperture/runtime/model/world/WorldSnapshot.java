package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable point-in-time input for behavior and future simulation consumers. */
public record WorldSnapshot(
	WorldRef world,
	Instant timestamp,
	long revision,
	Map<ArchitecturalObjectId, ObjectSnapshot> objects,
	Map<String, String> environment
) {
	public WorldSnapshot {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(timestamp, "timestamp");
		if (revision < 0) throw new IllegalArgumentException("World revision must be non-negative");
		objects = Map.copyOf(objects);
		environment = Map.copyOf(environment);
	}

	public Optional<ObjectSnapshot> object(ArchitecturalObjectId id) { return Optional.ofNullable(objects.get(id)); }

	public record ObjectSnapshot(ArchitecturalObjectInstance instance, RuntimeState state) {
		public ObjectSnapshot {
			Objects.requireNonNull(instance, "instance");
			Objects.requireNonNull(state, "state");
		}
	}
}
