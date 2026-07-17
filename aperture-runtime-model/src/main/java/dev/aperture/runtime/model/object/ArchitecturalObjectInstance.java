package dev.aperture.runtime.model.object;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Durable, family-neutral data for one placed architectural object. */
public record ArchitecturalObjectInstance(
	int schemaVersion,
	ArchitecturalObjectId objectId,
	ArchitecturalTypeId typeId,
	ArchitecturalFamilyId familyId,
	ParameterSet parameterOverrides,
	Transform3d transform,
	List<HostBinding> hostBindings,
	Map<String, Object> persistentState,
	long revision,
	Map<String, String> metadata
) {
	public ArchitecturalObjectInstance {
		if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be >= 1");
		Objects.requireNonNull(objectId, "objectId");
		Objects.requireNonNull(typeId, "typeId");
		Objects.requireNonNull(familyId, "familyId");
		Objects.requireNonNull(parameterOverrides, "parameterOverrides");
		Objects.requireNonNull(transform, "transform");
		hostBindings = List.copyOf(hostBindings);
		persistentState = Map.copyOf(persistentState);
		if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
		metadata = Map.copyOf(metadata);
	}

	public ArchitecturalObjectInstance withRevision(long newRevision) {
		return new ArchitecturalObjectInstance(schemaVersion, objectId, typeId, familyId,
			parameterOverrides, transform, hostBindings, persistentState, newRevision, metadata);
	}
}
