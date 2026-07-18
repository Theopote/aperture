package dev.aperture.runtime.transaction;

import dev.aperture.runtime.lifecycle.DefaultRuntimeObjectSession;
import dev.aperture.runtime.lifecycle.DirtyFlags;
import dev.aperture.runtime.lifecycle.RuntimeObjectRepository;
import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Builds and atomically installs one authoritative mutation for one object. */
public final class RuntimeTransaction {
	private final RuntimeObjectRepository repository;

	public RuntimeTransaction(RuntimeObjectRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	public RuntimeCommitResult commit(
		RuntimeObjectSession current,
		long expectedObjectRevision,
		RuntimeMutation mutation
	) {
		Objects.requireNonNull(current, "current");
		Objects.requireNonNull(mutation, "mutation");
		if (current.objectRevision() != expectedObjectRevision) {
			return RuntimeCommitResult.rejected("runtime.object_revision_conflict",
				"Expected object revision " + expectedObjectRevision + " but was " + current.objectRevision());
		}

		try {
			StateTransition transition = transition(current, mutation);
			if (mutation.isEmpty()) {
				return RuntimeCommitResult.rejected("runtime.empty_mutation", "Accepted command produced no mutation");
			}
			DefaultRuntimeObjectSession candidate = DefaultRuntimeObjectSession.committed(
				current, transition.current(), mutation.events().size(), dirtyFlags(current, transition));
			if (!repository.replace(current, candidate)) {
				return RuntimeCommitResult.rejected("runtime.concurrent_commit",
					"Authoritative session changed before commit");
			}
			return RuntimeCommitResult.committed(new CommittedRuntimeChange(
				current, candidate, transition, mutation.events(), mutation.worldEffects(), mutation.scheduledCommands()));
		} catch (RuntimeException failure) {
			return RuntimeCommitResult.rejected("runtime.state_invalid",
				failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
		}
	}

	private static StateTransition transition(RuntimeObjectSession session, RuntimeMutation mutation) {
		if (mutation.statePatches().isEmpty()) {
			return new StateTransition(session.state(), session.state(), java.util.Optional.empty());
		}
		Map<String, StateValue> updates = new LinkedHashMap<>();
		Instant timestamp = Instant.EPOCH;
		for (StatePatch patch : mutation.statePatches()) {
			if (!patch.expectedRevision().equals(session.stateRevision())) {
				throw new IllegalArgumentException("State patch expected revision " + patch.expectedRevision().value()
					+ " but was " + session.stateRevision().value());
			}
			for (Map.Entry<String, StateValue> update : patch.updates().entrySet()) {
				StateValue previous = updates.putIfAbsent(update.getKey(), update.getValue());
				if (previous != null && !previous.equals(update.getValue())) {
					throw new IllegalArgumentException("Conflicting updates for state property: " + update.getKey());
				}
			}
			if (patch.timestamp().isAfter(timestamp)) timestamp = patch.timestamp();
		}
		StatePatch merged = new StatePatch(session.stateRevision(), updates, timestamp);
		return new StateTransition(session.state(), session.state().apply(merged), java.util.Optional.of(merged));
	}

	private static DirtyFlags dirtyFlags(RuntimeObjectSession session, StateTransition transition) {
		if (!transition.changed()) return new DirtyFlags(false, true, false);
		boolean persistent = transition.appliedPatch().orElseThrow().updates().keySet().stream()
			.anyMatch(name -> session.state().schema().require(name).persistence()
				== dev.aperture.runtime.model.state.StatePersistence.PERSISTENT);
		return new DirtyFlags(persistent, true, true);
	}
}
