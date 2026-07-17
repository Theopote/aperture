package dev.aperture.runtime.state;

import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.pipeline.RuntimeObjectRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Persistence-facing state port used by the runtime object registry. */
public interface RuntimeStateStore extends RuntimeObjectRepository {
	Optional<ArchitecturalObject> find(UUID objectId);

	Collection<ArchitecturalObject> all();

	boolean remove(UUID objectId);
}
