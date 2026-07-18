package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;

/** Resolves family-owned runtime configuration without teaching the runtime about Door. */
@FunctionalInterface
public interface RuntimeObjectConfigurationResolver {
	RuntimeObjectConfiguration resolve(ArchitecturalObjectInstance instance);
}
