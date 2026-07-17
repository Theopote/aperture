package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Network-safe full object projection used to initialize or repair a replica. */
public record ReplicaSnapshot(
	ArchitecturalObjectInstance instance,
	Map<String, StateValue> distributedState,
	StateRevision stateRevision,
	Instant timestamp
) {
	public ReplicaSnapshot {
		Objects.requireNonNull(instance, "instance");
		distributedState = Map.copyOf(distributedState);
		Objects.requireNonNull(stateRevision, "stateRevision");
		Objects.requireNonNull(timestamp, "timestamp");
	}

	public static ReplicaSnapshot capture(ArchitecturalObjectInstance instance, RuntimeState state) {
		Map<String, StateValue> visible = new LinkedHashMap<>();
		state.values().forEach((name, value) -> {
			StateDistribution distribution = state.schema().require(name).distribution();
			if (distribution == StateDistribution.REPLICATED || distribution == StateDistribution.CLIENT_PREDICTED) {
				visible.put(name, value);
			}
		});
		ArchitecturalObjectInstance projection = new ArchitecturalObjectInstance(
			instance.schemaVersion(), instance.objectId(), instance.typeId(), instance.familyId(),
			instance.parameterOverrides(), instance.transform(), instance.hostBindings(), Map.of(),
			instance.revision(), instance.metadata());
		return new ReplicaSnapshot(projection, visible, state.revision(), state.timestamp());
	}

	public ReplicaObject restore(StateSchema schema) {
		Map<String, StateValue> values = new LinkedHashMap<>(schema.defaults());
		distributedState.forEach((name, value) -> {
			StateDistribution distribution = schema.require(name).distribution();
			if (distribution != StateDistribution.REPLICATED && distribution != StateDistribution.CLIENT_PREDICTED) {
				throw new IllegalArgumentException("Snapshot contains non-distributed property: " + name);
			}
			values.put(name, value);
		});
		return new ReplicaObject(instance, new RuntimeState(schema, values, stateRevision, timestamp));
	}
}
