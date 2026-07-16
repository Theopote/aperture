package dev.aperture.geometry.profile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;

/**
 * Registry of catalog profile definitions.
 */
public final class ProfileCatalogRegistry {
	private volatile Map<String, ProfileDefinition> byId = Map.of();
	private long revision;

	public synchronized void register(ProfileDefinition definition) {
		Map<String, ProfileDefinition> updated = new LinkedHashMap<>(byId);
		updated.put(definition.id(), definition);
		byId = Map.copyOf(updated);
		revision++;
	}

	public synchronized void replaceAll(Collection<ProfileDefinition> replacements) {
		Map<String, ProfileDefinition> updated = new LinkedHashMap<>();
		for (ProfileDefinition definition : replacements) {
			if (updated.put(definition.id(), definition) != null) {
				throw new IllegalArgumentException("Duplicate catalog profile: " + definition.id());
			}
		}
		byId = Map.copyOf(updated);
		revision++;
	}

	public Optional<ProfileDefinition> findById(String profileId) {
		return Optional.ofNullable(byId.get(profileId));
	}

	public ProfileDefinition requireById(String profileId) {
		return findById(profileId)
			.orElseThrow(() -> new IllegalArgumentException("Unknown catalog profile: " + profileId));
	}

	public long revision() {
		return revision;
	}

	public Map<String, ProfileDefinition> all() {
		return byId;
	}
}
