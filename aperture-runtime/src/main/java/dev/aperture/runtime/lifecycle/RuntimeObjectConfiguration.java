package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.behavior.BehaviorInstance;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateTickEvaluator;

import java.util.List;
import java.util.Objects;

/** Family-provided runtime data used to activate a generic object session. */
public record RuntimeObjectConfiguration(
	StateSchema stateSchema,
	CapabilityResolver capabilityResolver,
	List<BehaviorInstance> behaviors,
	KinematicModel kinematics,
	StateTickEvaluator tickEvaluator
) {
	public RuntimeObjectConfiguration(
		StateSchema stateSchema, CapabilityResolver capabilityResolver,
		List<BehaviorInstance> behaviors, KinematicModel kinematics
	) {
		this(stateSchema, capabilityResolver, behaviors, kinematics, StateTickEvaluator.NONE);
	}

	public RuntimeObjectConfiguration {
		Objects.requireNonNull(stateSchema, "stateSchema");
		Objects.requireNonNull(capabilityResolver, "capabilityResolver");
		behaviors = List.copyOf(behaviors);
		Objects.requireNonNull(kinematics, "kinematics");
		Objects.requireNonNull(tickEvaluator, "tickEvaluator");
	}

	@FunctionalInterface
	public interface CapabilityResolver {
		CapabilitySet resolve(dev.aperture.runtime.model.state.RuntimeState state);
	}
}
