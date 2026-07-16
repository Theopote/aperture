package dev.aperture.core.catalog;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.OpeningId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;

/**
 * In-memory registry of loaded opening type definitions.
 */
public final class OpeningTypeRegistry {
	private volatile Map<OpeningId, OpeningTypeDefinition> definitions = Map.of();
	private final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();

	public synchronized void register(OpeningTypeDefinition definition) {
		Map<OpeningId, OpeningTypeDefinition> updated = new LinkedHashMap<>(definitions);
		updated.put(definition.id(), definition);
		definitions = Map.copyOf(updated);
		revision.incrementAndGet();
	}

	public synchronized void replaceAll(Collection<OpeningTypeDefinition> replacements) {
		Map<OpeningId, OpeningTypeDefinition> updated = new LinkedHashMap<>();
		for (OpeningTypeDefinition definition : replacements) {
			if (updated.put(definition.id(), definition) != null) {
				throw new IllegalArgumentException("Duplicate opening type: " + definition.id());
			}
		}
		definitions = Map.copyOf(updated);
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

	public synchronized void clear() {
		definitions = Map.of();
		revision.incrementAndGet();
	}
}
