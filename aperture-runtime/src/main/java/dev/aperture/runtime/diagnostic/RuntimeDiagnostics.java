package dev.aperture.runtime.diagnostic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe operational counters for the architectural runtime loop. */
public final class RuntimeDiagnostics {
	private final AtomicLong interactions = new AtomicLong();
	private final AtomicLong rejectedInteractions = new AtomicLong();
	private final AtomicLong transitions = new AtomicLong();
	private final AtomicLong ticks = new AtomicLong();
	private final AtomicLong scheduledTasks = new AtomicLong();
	private final AtomicReference<String> lastFailure = new AtomicReference<>();

	public void recordInteraction(boolean changed) {
		interactions.incrementAndGet();
		if (changed) {
			transitions.incrementAndGet();
		}
	}

	public void recordRejected(Throwable failure) {
		rejectedInteractions.incrementAndGet();
		lastFailure.set(failure.getMessage());
	}

	public void recordTick(int executedTasks) {
		ticks.incrementAndGet();
		scheduledTasks.addAndGet(executedTasks);
	}

	public Snapshot snapshot() {
		return new Snapshot(
			interactions.get(),
			rejectedInteractions.get(),
			transitions.get(),
			ticks.get(),
			scheduledTasks.get(),
			lastFailure.get()
		);
	}

	public record Snapshot(
		long interactions,
		long rejectedInteractions,
		long transitions,
		long ticks,
		long scheduledTasks,
		String lastFailure
	) { }
}
