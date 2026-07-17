package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.Set;

/** Family-specific capability and state-transition strategy. */
public interface RuntimeBehavior<T extends ArchitecturalObject> {
	Class<T> objectType();

	Set<RuntimeCapability> capabilities(T object);

	RuntimeTransition<T> evaluate(T object, RuntimeInteraction interaction);
}
