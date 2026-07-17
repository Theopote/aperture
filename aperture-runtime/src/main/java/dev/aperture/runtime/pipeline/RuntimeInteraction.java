package dev.aperture.runtime.pipeline;

import java.util.Map;
import java.util.Objects;

/** Platform-neutral interaction submitted by a player, automation, or simulation. */
public record RuntimeInteraction(String action, RuntimeActor actor, Map<String, Object> inputs) {
	public RuntimeInteraction {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(actor, "actor");
		if (action.isBlank() || action.indexOf(':') <= 0 || action.endsWith(":")) {
			throw new IllegalArgumentException("Interaction action must be namespaced: " + action);
		}
		inputs = Map.copyOf(inputs);
	}

	public static RuntimeInteraction of(String action) {
		return new RuntimeInteraction(action, RuntimeActor.SYSTEM, Map.of());
	}

	public static RuntimeInteraction by(String action, RuntimeActor actor) {
		return new RuntimeInteraction(action, actor, Map.of());
	}
}
