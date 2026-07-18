package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.StateSchema;

/** Resolves the local schema required to materialize an authoritative snapshot. */
@FunctionalInterface
public interface StateSchemaResolver {
	StateSchema resolve(ArchitecturalObjectInstance instance);
}
