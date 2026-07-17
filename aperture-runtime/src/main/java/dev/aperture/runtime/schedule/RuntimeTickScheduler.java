package dev.aperture.runtime.schedule;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/** Deterministic single-clock scheduler advanced explicitly by the platform tick loop. */
public final class RuntimeTickScheduler {
	private final PriorityQueue<ScheduledTask> tasks = new PriorityQueue<>();
	private final AtomicLong sequence = new AtomicLong();
	private long currentTick;

	public synchronized long currentTick() {
		return currentTick;
	}

	public synchronized void schedule(long delayTicks, Runnable task) {
		if (delayTicks < 0) {
			throw new IllegalArgumentException("delayTicks must be non-negative");
		}
		tasks.add(new ScheduledTask(currentTick + delayTicks, sequence.getAndIncrement(), Objects.requireNonNull(task, "task")));
	}

	public synchronized int tick() {
		currentTick++;
		int executed = 0;
		while (!tasks.isEmpty() && tasks.peek().dueTick() <= currentTick) {
			tasks.remove().task().run();
			executed++;
		}
		return executed;
	}

	private record ScheduledTask(long dueTick, long sequence, Runnable task) implements Comparable<ScheduledTask> {
		@Override
		public int compareTo(ScheduledTask other) {
			int byTick = Long.compare(dueTick, other.dueTick);
			return byTick != 0 ? byTick : Long.compare(sequence, other.sequence);
		}
	}
}
