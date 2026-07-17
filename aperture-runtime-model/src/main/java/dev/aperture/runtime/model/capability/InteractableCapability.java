package dev.aperture.runtime.model.capability;

import java.util.Set;

public interface InteractableCapability extends Capability {
	boolean enabled();
	Set<String> interactionIds();
}
