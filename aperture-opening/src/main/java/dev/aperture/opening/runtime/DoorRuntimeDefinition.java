package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectDefinition;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;

import java.util.Map;
import java.util.Objects;

/** Runtime definition for the first Door vertical slice. */
public record DoorRuntimeDefinition(ArchitecturalTypeId typeId, Map<String, String> metadata)
	implements ArchitecturalObjectDefinition {
	public static final ArchitecturalFamilyId OPENING_FAMILY = new ArchitecturalFamilyId("aperture:opening");

	public DoorRuntimeDefinition {
		Objects.requireNonNull(typeId, "typeId");
		metadata = Map.copyOf(metadata);
	}

	@Override public int schemaVersion() { return 1; }
	@Override public ArchitecturalFamilyId familyId() { return OPENING_FAMILY; }
}
