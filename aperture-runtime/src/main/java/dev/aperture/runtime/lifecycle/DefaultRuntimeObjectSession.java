package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.behavior.BehaviorInstance;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.persistence.BehaviorConfiguration;
import dev.aperture.runtime.model.state.RuntimeState;

import java.util.List;
import java.util.Objects;

/** Default immutable-data-backed authoritative session. */
public final class DefaultRuntimeObjectSession implements RuntimeObjectSession {
	private final ArchitecturalObjectInstance instance;
	private final RuntimeState state;
	private final RuntimeObjectConfiguration configuration;
	private final long eventSequence;
	private final DirtyFlags dirtyFlags;

	private DefaultRuntimeObjectSession(
		ArchitecturalObjectInstance instance,
		RuntimeState state,
		RuntimeObjectConfiguration configuration,
		long eventSequence,
		DirtyFlags dirtyFlags
	) {
		this.instance = Objects.requireNonNull(instance, "instance");
		this.state = Objects.requireNonNull(state, "state");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		if (eventSequence < 0) throw new IllegalArgumentException("eventSequence must be non-negative");
		this.eventSequence = eventSequence;
		this.dirtyFlags = Objects.requireNonNull(dirtyFlags, "dirtyFlags");
	}

	public static DefaultRuntimeObjectSession create(
		ArchitecturalObjectInstance instance, RuntimeObjectConfiguration configuration
	) {
		return new DefaultRuntimeObjectSession(instance, RuntimeState.initial(configuration.stateSchema()),
			configuration, 0, DirtyFlags.CREATED);
	}

	public static DefaultRuntimeObjectSession restore(
		ArchitecturalObjectSnapshot snapshot, RuntimeObjectConfiguration configuration
	) {
		return new DefaultRuntimeObjectSession(snapshot.instance(), snapshot.restoreState(configuration.stateSchema()),
			configuration, 0, DirtyFlags.CLEAN);
	}

	@Override public ArchitecturalObjectInstance instance() { return instance; }
	@Override public RuntimeState state() { return state; }
	@Override public CapabilitySet capabilities() { return configuration.capabilityResolver().resolve(state); }
	@Override public List<BehaviorInstance> behaviors() { return configuration.behaviors(); }
	@Override public KinematicModel kinematics() { return configuration.kinematics(); }
	@Override public long eventSequence() { return eventSequence; }
	@Override public DirtyFlags dirtyFlags() { return dirtyFlags; }

	@Override
	public ArchitecturalObjectSnapshot snapshot() {
		List<BehaviorConfiguration> behaviorConfigurations = behaviors().stream()
			.map(behavior -> new BehaviorConfiguration(behavior.definition().id(),
				behavior.definition().version(), behavior.definition().configuration()))
			.toList();
		return ArchitecturalObjectSnapshot.capture(instance, state, behaviorConfigurations);
	}
}
