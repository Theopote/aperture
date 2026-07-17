package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.behavior.RuntimeBehaviorEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Platform-neutral runtime pipeline: capability resolution, behavior evaluation,
 * state transition, repository commit, and persistence/replication requests.
 */
public final class RuntimePipeline {
	private final RuntimeBehaviorEngine behaviorEngine;
	private final RuntimeObjectRepository repository;

	public RuntimePipeline(List<RuntimeBehavior<?>> behaviors, RuntimeObjectRepository repository) {
		this.behaviorEngine = new RuntimeBehaviorEngine(behaviors);
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	public Set<RuntimeCapability> capabilities(ArchitecturalObject object) {
		return behaviorEngine.capabilities(Objects.requireNonNull(object, "object"));
	}

	public RuntimeResult process(ArchitecturalObject object, RuntimeInteraction interaction) {
		Objects.requireNonNull(object, "object");
		Objects.requireNonNull(interaction, "interaction");

		RuntimeTransition<ArchitecturalObject> transition = behaviorEngine.evaluate(object, interaction);
		ArchitecturalObject current = transition.object();
		validateTransition(object, current);

		if (object.equals(current)) {
			return new RuntimeResult(object, current, behaviorEngine.capabilities(current), List.of());
		}

		repository.save(current);
		List<RuntimeEffect> effects = new ArrayList<>(transition.worldEffects());
		effects.add(new RuntimeEffect.PersistenceRequested(current));
		effects.add(new RuntimeEffect.ReplicationRequested(current));
		return new RuntimeResult(object, current, behaviorEngine.capabilities(current), effects);
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
