package dev.aperture.core.instance;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory instance store for development, testing, and pre-world-save Phase 0.
 */
public final class InMemoryOpeningInstanceStore implements OpeningInstanceStore {
	private final Map<UUID, OpeningInstance> instances = new ConcurrentHashMap<>();

	@Override
	public Optional<OpeningInstance> findById(UUID instanceId) {
		return Optional.ofNullable(instances.get(instanceId));
	}

	@Override
	public Collection<OpeningInstance> all() {
		return instances.values();
	}

	@Override
	public void put(OpeningInstance instance) {
		instances.put(instance.instanceId(), instance);
	}

	@Override
	public boolean remove(UUID instanceId) {
		return instances.remove(instanceId) != null;
	}

	public void clear() {
		instances.clear();
	}
}
