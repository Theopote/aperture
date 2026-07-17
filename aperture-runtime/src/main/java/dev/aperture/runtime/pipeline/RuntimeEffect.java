package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Explicit output consumed by platform adapters after a runtime transition.
 * The runtime describes effects but never imports or mutates a Minecraft world.
 */
public sealed interface RuntimeEffect {
	record GeometryInvalidated(UUID objectId) implements RuntimeEffect {
		public GeometryInvalidated {
			Objects.requireNonNull(objectId, "objectId");
		}
	}

	record PersistenceRequested(ArchitecturalObject snapshot) implements RuntimeEffect {
		public PersistenceRequested {
			Objects.requireNonNull(snapshot, "snapshot");
		}
	}

	record ReplicationRequested(ArchitecturalObject snapshot) implements RuntimeEffect {
		public ReplicationRequested {
			Objects.requireNonNull(snapshot, "snapshot");
		}
	}
}
