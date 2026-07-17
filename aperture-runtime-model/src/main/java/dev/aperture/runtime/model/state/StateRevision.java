package dev.aperture.runtime.model.state;

/** Monotonic optimistic-concurrency token for one runtime state stream. */
public record StateRevision(long value) {
	public static final StateRevision INITIAL = new StateRevision(0);

	public StateRevision {
		if (value < 0) throw new IllegalArgumentException("State revision must be non-negative");
	}

	public StateRevision next() { return new StateRevision(Math.addExact(value, 1)); }
}
