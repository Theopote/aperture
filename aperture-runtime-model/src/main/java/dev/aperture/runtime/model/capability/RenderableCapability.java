package dev.aperture.runtime.model.capability;

public interface RenderableCapability extends Capability {
	String renderDefinitionId();
	boolean visible();
}
