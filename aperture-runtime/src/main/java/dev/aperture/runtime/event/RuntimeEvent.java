package dev.aperture.runtime.event;

import dev.aperture.runtime.pipeline.RuntimeResult;

import java.util.Objects;
import java.util.UUID;

/** Events emitted by the runtime environment after observable lifecycle changes. */
public interface RuntimeEvent {
	record InteractionCompleted(RuntimeResult result) implements RuntimeEvent {
		public InteractionCompleted {
			Objects.requireNonNull(result, "result");
		}
	}

	record InteractionRejected(UUID objectId, String action, String reason) implements RuntimeEvent {
		public InteractionRejected {
			Objects.requireNonNull(objectId, "objectId");
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(reason, "reason");
		}
	}

	record TickAdvanced(long tick, int executedTasks) implements RuntimeEvent {
		public TickAdvanced {
			if (tick < 0 || executedTasks < 0) {
				throw new IllegalArgumentException("Tick values must be non-negative");
			}
		}
	}
}
