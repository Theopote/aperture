package dev.aperture.runtime.pipeline;

import dev.aperture.runtime.world.RuntimeWorldQuery;

import java.util.Objects;

/** Read-only environment available during capability and behavior evaluation. */
public record RuntimeEvaluationContext(long tick, RuntimeWorldQuery world) {
	public RuntimeEvaluationContext {
		if (tick < 0) {
			throw new IllegalArgumentException("tick must be non-negative");
		}
		Objects.requireNonNull(world, "world");
	}

	public static RuntimeEvaluationContext empty() {
		return new RuntimeEvaluationContext(0, RuntimeWorldQuery.empty());
	}
}
