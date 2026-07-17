package dev.aperture.core.instance;

import dev.aperture.core.state.RuntimeState;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Opening compatibility view over the schema-backed {@link RuntimeState}.
 * Family behavior should update named runtime properties rather than add fields here.
 */
public record OpeningState(RuntimeState runtimeState) {
	public static final OpeningState CLOSED = new OpeningState(RuntimeState.initial(OpeningStateSchemas.OPERABLE));

	public OpeningState {
		Objects.requireNonNull(runtimeState, "runtimeState");
	}

	public OpeningState(double openRatio) {
		this(RuntimeState.initial(OpeningStateSchemas.OPERABLE, Map.of("openRatio", openRatio)));
	}

	public double openRatio() {
		return runtimeState.number("openRatio");
	}

	public boolean locked() {
		return runtimeState.bool("locked");
	}

	public OpeningState transition(
		Map<String, Object> persistentUpdates,
		Map<String, Object> transientUpdates,
		Instant timestamp
	) {
		return new OpeningState(runtimeState.transition(persistentUpdates, transientUpdates, timestamp));
	}
}
