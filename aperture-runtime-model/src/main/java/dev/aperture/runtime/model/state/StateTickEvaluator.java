package dev.aperture.runtime.model.state;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Family-owned pure tick rule; authoritative application remains in RuntimeTransaction. */
@FunctionalInterface
public interface StateTickEvaluator {
	StateTickEvaluator NONE = (state, elapsed, timestamp) -> Optional.empty();

	Optional<StatePatch> evaluate(RuntimeState state, Duration elapsed, Instant timestamp);
}
