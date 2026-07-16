package dev.aperture.core.catalog;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.OpeningId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of loaded opening type definitions.
 */
public final class OpeningTypeRegistry {
	private final Map<OpeningId, OpeningTypeDefinition> definitions = new ConcurrentHashMap<>();
	private final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();

	public void register(OpeningTypeDefinition definition) {
		definitions.put(definition.id(), definition);
		revision.incrementAndGet();
	}

	public Optional<OpeningTypeDefinition> get(OpeningId id) {
		return Optional.ofNullable(definitions.get(id));
	}

	public OpeningTypeDefinition require(OpeningId id) {
		return get(id).orElseThrow(() -> new IllegalArgumentException("Unknown opening type: " + id));
	}

	public Collection<OpeningTypeDefinition> all() {
		return definitions.values();
	}

	public long revision() {
		return revision.get();
	}

	public void clear() {
		definitions.clear();
		revision.incrementAndGet();
	}
}
