package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Platform-neutral runtime pipeline: capability resolution, behavior evaluation,
 * state transition, repository commit, and persistence/replication requests.
 */
public final class RuntimePipeline {
	private final List<RuntimeBehavior<?>> behaviors;
	private final RuntimeObjectRepository repository;

	public RuntimePipeline(List<RuntimeBehavior<?>> behaviors, RuntimeObjectRepository repository) {
		this.behaviors = List.copyOf(behaviors);
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	public RuntimeResult process(ArchitecturalObject object, RuntimeInteraction interaction) {
		Objects.requireNonNull(object, "object");
		Objects.requireNonNull(interaction, "interaction");

		RuntimeBehavior<ArchitecturalObject> behavior = behaviorFor(object);
		RuntimeTransition<ArchitecturalObject> transition = behavior.evaluate(object, interaction);
		ArchitecturalObject current = transition.object();
		validateTransition(object, current);

		if (object.equals(current)) {
			return new RuntimeResult(object, current, behavior.capabilities(current), List.of());
		}

		repository.save(current);
		List<RuntimeEffect> effects = new ArrayList<>(transition.worldEffects());
		effects.add(new RuntimeEffect.PersistenceRequested(current));
		effects.add(new RuntimeEffect.ReplicationRequested(current));
		return new RuntimeResult(object, current, behavior.capabilities(current), effects);
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

	private static void validateTransition(ArchitecturalObject previous, ArchitecturalObject current) {
		if (!previous.instanceId().equals(current.instanceId())) {
			throw new IllegalStateException("Runtime behavior changed object identity");
		}
		if (!previous.equals(current) && current.revision() <= previous.revision()) {
			throw new IllegalStateException("Changed runtime state must advance revision");
		}
	}
}
