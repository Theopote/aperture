package dev.aperture.runtime.pipeline;

import java.util.Map;
import java.util.Objects;

/** Platform-neutral interaction submitted by a player, automation, or simulation. */
public record RuntimeInteraction(String action, Map<String, Object> inputs) {
	public RuntimeInteraction {
		Objects.requireNonNull(action, "action");
		if (action.isBlank() || action.indexOf(':') <= 0 || action.endsWith(":")) {
			throw new IllegalArgumentException("Interaction action must be namespaced: " + action);
		}
		inputs = Map.copyOf(inputs);
	}

	public static RuntimeInteraction of(String action) {
		return new RuntimeInteraction(action, Map.of());
	}
}
