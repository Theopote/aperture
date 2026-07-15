package dev.aperture.geometry.profile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of catalog profile definitions.
 */
public final class ProfileCatalogRegistry {
	private final Map<String, ProfileDefinition> byId = new LinkedHashMap<>();

	public void register(ProfileDefinition definition) {
		byId.put(definition.id(), definition);
	}

	public Optional<ProfileDefinition> findById(String profileId) {
		return Optional.ofNullable(byId.get(profileId));
	}

	public ProfileDefinition requireById(String profileId) {
		return findById(profileId)
			.orElseThrow(() -> new IllegalArgumentException("Unknown catalog profile: " + profileId));
	}

	public Map<String, ProfileDefinition> all() {
		return Collections.unmodifiableMap(byId);
	}
}
