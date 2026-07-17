package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StateValue;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the network-safe projection of a committed server transition. */
public final class StateDeltaFactory {
	private StateDeltaFactory() { }

	public static StateDeltaMessage between(
		ArchitecturalObjectInstance previousInstance,
		RuntimeState previousState,
		ArchitecturalObjectInstance currentInstance,
		RuntimeState currentState
	) {
		if (!previousInstance.objectId().equals(currentInstance.objectId())) {
			throw new IllegalArgumentException("Cannot diff different objects");
		}
		if (currentInstance.revision() != previousInstance.revision() + 1) {
			throw new IllegalArgumentException("Object transition is not contiguous");
		}
		if (currentState.revision().value() != previousState.revision().value() + 1) {
			throw new IllegalArgumentException("State transition is not contiguous");
		}
		Map<String, StateValue> updates = new LinkedHashMap<>();
		currentState.values().forEach((name, value) -> {
			StateDistribution distribution = currentState.schema().require(name).distribution();
			boolean visible = distribution == StateDistribution.REPLICATED
				|| distribution == StateDistribution.CLIENT_PREDICTED;
			if (visible && !value.equals(previousState.value(name))) updates.put(name, value);
		});
		return new StateDeltaMessage(1, currentInstance.objectId(), previousInstance.revision(),
			currentInstance.revision(), previousState.revision(), currentState.revision(), updates,
			currentState.timestamp());
	}
}
