package dev.aperture.kernel.internal;

import dev.aperture.kernel.KernelStats;
import dev.aperture.kernel.OpeningResult;
import dev.aperture.pipeline.PipelineCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and aggregates statistics for the kernel.
 * <p>
 * Thread-safe using atomic operations.
 * Package-private, not part of public API.
 */
public final class StatsCollector {

	private final AtomicLong totalRequests = new AtomicLong(0);
	private final AtomicLong successfulRequests = new AtomicLong(0);
	private final AtomicLong failedRequests = new AtomicLong(0);
	private final LongAdder totalExecutionTime = new LongAdder();
	private final Map<String, AtomicLong> failuresByStage = new ConcurrentHashMap<>();

	private volatile PipelineCache.CacheStats lastCacheStats =
		new PipelineCache.CacheStats(0, 0, 0, 0);

	/**
	 * Record a completed request.
	 */
	public void record(OpeningResult result, java.time.Duration executionTime) {
		totalRequests.incrementAndGet();
		totalExecutionTime.add(executionTime.toNanos());

		if (result.isSuccess()) {
			successfulRequests.incrementAndGet();
		} else {
			failedRequests.incrementAndGet();

			if (result instanceof OpeningResult.Failure failure) {
				String stage = failure.failedStage();
				failuresByStage.computeIfAbsent(stage, k -> new AtomicLong(0))
					.incrementAndGet();
			}
		}
	}

	/**
	 * Record a failure.
	 */
	public void recordFailure(String typeId, String stage) {
		totalRequests.incrementAndGet();
		failedRequests.incrementAndGet();
		failuresByStage.computeIfAbsent(stage, k -> new AtomicLong(0))
			.incrementAndGet();
	}

	/**
	 * Update cache statistics snapshot.
	 */
	public void updateCacheStats(PipelineCache.CacheStats stats) {
		this.lastCacheStats = stats;
	}

	/**
	 * Get current statistics.
	 */
	public KernelStats getStats() {
		long total = totalRequests.get();
		double avgTime = total == 0 ? 0.0 :
			(double) totalExecutionTime.sum() / total / 1_000_000.0;

		// Convert ConcurrentHashMap<String, AtomicLong> to Map<String, Long>
		Map<String, Long> failures = new ConcurrentHashMap<>();
		failuresByStage.forEach((stage, count) ->
			failures.put(stage, count.get()));

		return new KernelStats(
			total,
			successfulRequests.get(),
			failedRequests.get(),
			avgTime,
			lastCacheStats,
			failures
		);
	}

	/**
	 * Reset all statistics.
	 */
	public void reset() {
		totalRequests.set(0);
		successfulRequests.set(0);
		failedRequests.set(0);
		totalExecutionTime.reset();
		failuresByStage.clear();
		// Keep cache stats - those are managed by the cache itself
	}
}
