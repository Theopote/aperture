package dev.aperture.runtime.model.persistence;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;

/** Platform persistence port; implementations may use NBT, JSON, databases, or test memory. */
public interface ArchitecturalObjectPersistence {
	void save(ArchitecturalObjectSnapshot snapshot);
	Optional<ArchitecturalObjectSnapshot> load(ArchitecturalObjectId objectId);
	void delete(ArchitecturalObjectId objectId);
}
