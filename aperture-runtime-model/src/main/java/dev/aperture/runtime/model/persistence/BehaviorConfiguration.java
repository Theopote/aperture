package dev.aperture.runtime.model.persistence;

import dev.aperture.runtime.model.behavior.BehaviorId;

import java.util.Map;
import java.util.Objects;

/** Durable behavior binding; executable BehaviorInstance is reconstructed at activation. */
public record BehaviorConfiguration(BehaviorId behaviorId, int version, Map<String, Object> values) {
	public BehaviorConfiguration {
		Objects.requireNonNull(behaviorId, "behaviorId");
		if (version < 1) throw new IllegalArgumentException("Behavior configuration version must be >= 1");
		values = Map.copyOf(values);
	}
}
