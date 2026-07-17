package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

/** Commits an evaluated runtime snapshot before effects are exposed to adapters. */
@FunctionalInterface
public interface RuntimeObjectRepository {
	void save(ArchitecturalObject object);
}
