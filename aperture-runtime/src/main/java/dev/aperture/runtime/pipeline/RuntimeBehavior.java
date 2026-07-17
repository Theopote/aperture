package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.Set;

/** Family-specific capability and state-transition strategy. */
public interface RuntimeBehavior<T extends ArchitecturalObject> {
	Class<T> objectType();

	Set<RuntimeCapability> capabilities(T object);
	default Set<RuntimeCapability> capabilities(T object, RuntimeEvaluationContext context) {
		return capabilities(object);
	}


	RuntimeTransition<T> evaluate(T object, RuntimeInteraction interaction);

	default RuntimeTransition<T> evaluate(
		T object,
		RuntimeInteraction interaction,
		RuntimeEvaluationContext context
	) {
		return evaluate(object, interaction);
	}
}
