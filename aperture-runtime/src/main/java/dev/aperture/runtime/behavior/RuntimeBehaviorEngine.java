package dev.aperture.runtime.behavior;

import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.pipeline.RuntimeBehavior;
import dev.aperture.runtime.pipeline.RuntimeCapability;
import dev.aperture.runtime.pipeline.RuntimeInteraction;
import dev.aperture.runtime.pipeline.RuntimeEvaluationContext;
import dev.aperture.runtime.pipeline.RuntimeTransition;

import java.util.List;
import java.util.Set;

/** Resolves the single family behavior responsible for an object and evaluates it. */
public final class RuntimeBehaviorEngine {
	private final List<RuntimeBehavior<?>> behaviors;

	public RuntimeBehaviorEngine(List<RuntimeBehavior<?>> behaviors) {
		this.behaviors = List.copyOf(behaviors);
	}

	public Set<RuntimeCapability> capabilities(ArchitecturalObject object) {
		return capabilities(object, RuntimeEvaluationContext.empty());
	}

	public Set<RuntimeCapability> capabilities(ArchitecturalObject object, RuntimeEvaluationContext context) {
		return behaviorFor(object).capabilities(object, context);
	}

	public RuntimeTransition<ArchitecturalObject> evaluate(
		ArchitecturalObject object,
		RuntimeInteraction interaction,
		RuntimeEvaluationContext context
	) {
		RuntimeBehavior<ArchitecturalObject> behavior = behaviorFor(object);
		Set<RuntimeCapability> available = behavior.capabilities(object, context);
		if (available.stream().noneMatch(capability -> capability.id().equals(interaction.action()))) {
			throw new IllegalArgumentException(
				"Object does not expose capability " + interaction.action() + ": " + object.instanceId()
			);
		}
		return behavior.evaluate(object, interaction, context);
	}

	@SuppressWarnings("unchecked")
	private RuntimeBehavior<ArchitecturalObject> behaviorFor(ArchitecturalObject object) {
		RuntimeBehavior<?> match = null;
		for (RuntimeBehavior<?> candidate : behaviors) {
			if (candidate.objectType().isInstance(object)) {
				if (match != null) {
					throw new IllegalStateException("Multiple runtime behaviors match " + object.getClass().getName());
				}
				match = candidate;
			}
		}
		if (match == null) {
			throw new IllegalArgumentException("No runtime behavior for " + object.getClass().getName());
		}
		return (RuntimeBehavior<ArchitecturalObject>) match;
	}
}
