package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Pure kinematic driver that advances openRatio toward targetOpenRatio. */
public final class DoorRuntimeTick {
	private DoorRuntimeTick() { }

	public static dev.aperture.runtime.model.state.StateTickEvaluator atSpeed(double speedPerSecond) {
		if (!Double.isFinite(speedPerSecond) || speedPerSecond <= 0) {
			throw new IllegalArgumentException("speedPerSecond must be positive");
		}
		return (state, elapsed, time) -> advance(state, elapsed, speedPerSecond, time);
	}

	public static Optional<StatePatch> advance(RuntimeState state, Duration elapsed, double speedPerSecond, Instant time) {
		if (elapsed.isNegative()) throw new IllegalArgumentException("elapsed must not be negative");
		if (!Double.isFinite(speedPerSecond) || speedPerSecond <= 0) throw new IllegalArgumentException("speedPerSecond must be positive");
		double current = DoorStateSchema.number(state, DoorStateSchema.OPEN_RATIO);
		double target = DoorStateSchema.number(state, DoorStateSchema.TARGET_OPEN_RATIO);
		if (current == target) return Optional.empty();
		double step = speedPerSecond * elapsed.toNanos() / 1_000_000_000.0;
		double next = current < target ? Math.min(target, current + step) : Math.max(target, current - step);
		String motion = next == target ? "idle" : next > current ? "opening" : "closing";
		return Optional.of(new StatePatch(state.revision(), Map.of(
			DoorStateSchema.OPEN_RATIO, StateValue.number(next),
			DoorStateSchema.MOTION, StateValue.enumeration(motion)
		), time));
	}
}
