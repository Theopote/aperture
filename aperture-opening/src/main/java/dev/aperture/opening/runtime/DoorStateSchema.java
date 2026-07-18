package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StatePersistence;
import dev.aperture.runtime.model.state.StatePropertyDefinition;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;

import java.util.Set;

public final class DoorStateSchema {
	public static final String OPEN_RATIO = "openRatio";
	public static final String TARGET_OPEN_RATIO = "targetOpenRatio";
	public static final String LOCKED = "locked";
	public static final String MOTION = "motion";
	public static final String ENABLED = "enabled";
	public static final String LAST_INTERACTOR = "lastInteractor";

	public static final StateSchema SCHEMA = StateSchema.builder("aperture:door", 1)
		.property(OPEN_RATIO, StatePropertyDefinition.number(
			0, 0.0, 1.0, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property(TARGET_OPEN_RATIO, StatePropertyDefinition.number(
			0, 0.0, 1.0, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property(LOCKED, StatePropertyDefinition.bool(
			false, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property(MOTION, StatePropertyDefinition.enumeration(
			"idle", Set.of("idle", "opening", "closing", "blocked"),
			StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
		.property(ENABLED, StatePropertyDefinition.bool(
			true, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property(LAST_INTERACTOR, new StatePropertyDefinition(
			dev.aperture.runtime.model.state.StateValueType.STRING, StateValue.string(""),
			StatePersistence.TRANSIENT, StateDistribution.SERVER_ONLY, null, null, Set.of()))
		.build();

	private DoorStateSchema() { }

	public static double number(dev.aperture.runtime.model.state.RuntimeState state, String property) {
		return ((StateValue.NumberValue) state.value(property)).value();
	}

	public static boolean bool(dev.aperture.runtime.model.state.RuntimeState state, String property) {
		return ((StateValue.BooleanValue) state.value(property)).value();
	}
}
