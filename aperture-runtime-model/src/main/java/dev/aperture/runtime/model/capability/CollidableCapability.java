package dev.aperture.runtime.model.capability;

public interface CollidableCapability extends Capability {
	String collisionDefinitionId();
	boolean collisionEnabled();
}
