package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.behavior.BehaviorInstance;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.RuntimeArchitecturalObject;
import dev.aperture.runtime.model.state.RuntimeState;

import java.util.List;
import java.util.Objects;

public record DoorRuntimeObject(
	DoorRuntimeDefinition definition,
	ArchitecturalObjectInstance instance,
	RuntimeState state,
	CapabilitySet capabilities,
	List<BehaviorInstance> behaviors
) implements RuntimeArchitecturalObject {
	public DoorRuntimeObject {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(instance, "instance");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(capabilities, "capabilities");
		behaviors = List.copyOf(behaviors);
		if (!definition.typeId().equals(instance.typeId())) throw new IllegalArgumentException("Definition and instance type differ");
		if (!DoorRuntimeDefinition.OPENING_FAMILY.equals(instance.familyId())) throw new IllegalArgumentException("Door must use Opening family");
	}

	public static DoorRuntimeObject activate(DoorRuntimeDefinition definition, ArchitecturalObjectInstance instance) {
		RuntimeState state = RuntimeState.restore(
			DoorStateSchema.SCHEMA, persistentState(instance),
			new dev.aperture.runtime.model.state.StateRevision(instance.revision()), java.time.Instant.EPOCH);
		return new DoorRuntimeObject(definition, instance, state, DoorCapabilities.from(state),
			List.of(new ManualDoorInteractionBehavior()));
	}

	private static java.util.Map<String, dev.aperture.runtime.model.state.StateValue> persistentState(
		ArchitecturalObjectInstance instance
	) {
		java.util.Map<String, dev.aperture.runtime.model.state.StateValue> result = new java.util.LinkedHashMap<>();
		instance.persistentState().forEach((name, value) -> {
			if (value instanceof dev.aperture.runtime.model.state.StateValue stateValue) result.put(name, stateValue);
		});
		return java.util.Map.copyOf(result);
	}
}
