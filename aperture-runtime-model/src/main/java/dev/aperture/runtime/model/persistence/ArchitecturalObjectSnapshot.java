package dev.aperture.runtime.model.persistence;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete platform-neutral durable payload for one activated architectural object. */
public record ArchitecturalObjectSnapshot(
	int schemaVersion,
	ArchitecturalObjectInstance instance,
	Map<String, StateValue> persistentState,
	StateRevision stateRevision,
	Instant stateTimestamp,
	List<BehaviorConfiguration> behaviors
) {
	public ArchitecturalObjectSnapshot {
		if (schemaVersion < 1) throw new IllegalArgumentException("Snapshot schemaVersion must be >= 1");
		Objects.requireNonNull(instance, "instance");
		persistentState = Map.copyOf(persistentState);
		Objects.requireNonNull(stateRevision, "stateRevision");
		Objects.requireNonNull(stateTimestamp, "stateTimestamp");
		behaviors = List.copyOf(behaviors);
	}

	public static ArchitecturalObjectSnapshot capture(
		ArchitecturalObjectInstance instance,
		RuntimeState state,
		List<BehaviorConfiguration> behaviors
	) {
		Map<String, StateValue> persistent = state.persistentValues();
		Map<String, Object> instanceState = new LinkedHashMap<>();
		instanceState.putAll(persistent);
		ArchitecturalObjectInstance durableInstance = new ArchitecturalObjectInstance(
			instance.schemaVersion(), instance.objectId(), instance.typeId(), instance.familyId(),
			instance.parameterOverrides(), instance.transform(), instance.hostBindings(), instanceState,
			instance.revision(), instance.metadata());
		return new ArchitecturalObjectSnapshot(1, durableInstance, persistent,
			state.revision(), state.timestamp(), behaviors);
	}

	public RuntimeState restoreState(StateSchema schema) {
		for (String property : persistentState.keySet()) {
			if (schema.require(property).persistence() != dev.aperture.runtime.model.state.StatePersistence.PERSISTENT) {
				throw new IllegalArgumentException("Snapshot contains non-persistent property: " + property);
			}
		}
		return RuntimeState.restore(schema, persistentState, stateRevision, stateTimestamp);
	}
}
