package dev.aperture.runtime.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RuntimeTickContext(Duration elapsed, Instant timestamp) {
	public RuntimeTickContext {
		Objects.requireNonNull(elapsed, "elapsed");
		if (elapsed.isNegative()) throw new IllegalArgumentException("elapsed must not be negative");
		Objects.requireNonNull(timestamp, "timestamp");
	}
}
