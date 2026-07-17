package dev.aperture.runtime.model.object;

import java.util.Map;

/** Immutable family-neutral identity and metadata of executable design intent. */
public interface ArchitecturalObjectDefinition {
	int schemaVersion();

	ArchitecturalTypeId typeId();

	ArchitecturalFamilyId familyId();

	default Map<String, String> metadata() {
		return Map.of();
	}
}
