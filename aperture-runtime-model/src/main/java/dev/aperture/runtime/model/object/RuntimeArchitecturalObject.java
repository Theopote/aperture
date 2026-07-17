package dev.aperture.runtime.model.object;

/** Activated execution view of a durable architectural object instance. */
public interface RuntimeArchitecturalObject {
	ArchitecturalObjectDefinition definition();

	ArchitecturalObjectInstance instance();

	default ArchitecturalObjectId objectId() {
		return instance().objectId();
	}

	default ArchitecturalTypeId typeId() {
		return instance().typeId();
	}
}
