package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Collection;
import java.util.Optional;

/** Authoritative active-session repository. */
public interface RuntimeObjectRepository {
	RuntimeObjectSession add(RuntimeObjectSession session);
	Optional<RuntimeObjectSession> find(ArchitecturalObjectId objectId);
	RuntimeObjectSession require(ArchitecturalObjectId objectId);
	boolean replace(RuntimeObjectSession expected, RuntimeObjectSession replacement);
	Optional<RuntimeObjectSession> unload(ArchitecturalObjectId objectId);
	Collection<RuntimeObjectSession> activeObjects();
}
