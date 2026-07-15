package dev.aperture.core.instance;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for placed opening instances.
 * Minecraft world save adapters implement this in the Fabric module.
 */
public interface OpeningInstanceStore {
	Optional<OpeningInstance> findById(UUID instanceId);

	Collection<OpeningInstance> all();

	void put(OpeningInstance instance);

	boolean remove(UUID instanceId);
}
