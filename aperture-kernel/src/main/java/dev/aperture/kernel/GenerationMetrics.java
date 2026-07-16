package dev.aperture.kernel;

import java.util.Map;
import java.util.Objects;

/**
 * Metrics collected during opening generation.
 * <p>
 * Includes timing information, cache statistics, and stage-level details.
 */
public record GenerationMetrics(
	long totalTimeMs,
	int cacheHits,
	int cacheMisses,
	Map<String, Long> stageTimings
) {
	public GenerationMetrics {
		Objects.requireNonNull(stageTimings, "stageTimings cannot be null");
		stageTimings = Map.copyOf(stageTimings); // Immutable
	}

	/**
	 * Get cache hit rate (0.0 to 1.0).
	 */
	public double cacheHitRate() {
		int total = cacheHits + cacheMisses;
		return total == 0 ? 0.0 : (double) cacheHits / total;
	}

	/**
	 * Get timing for a specific stage.
	 */
	public long getStageTime(String stageName) {
		return stageTimings.getOrDefault(stageName, 0L);
	}

	/**
	 * Get the slowest stage name.
	 */
	public String getSlowestStage() {
		return stageTimings.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse("unknown");
	}

	/**
	 * Get the slowest stage time.
	 */
	public long getSlowestStageTime() {
		return stageTimings.values().stream()
			.mapToLong(Long::longValue)
			.max()
			.orElse(0L);
	}

	/**
	 * Calculate speedup from caching.
	 * <p>
	 * Returns estimated execution time without cache / actual execution time.
	 * Returns 1.0 if no cache hits.
	 */
	public double cacheSpeedup() {
		if (cacheHits == 0) {
			return 1.0;
		}
		// Estimate: each cache hit saves ~80% of stage time
		long savedTime = cacheHits * (totalTimeMs / Math.max(1, cacheHits + cacheMisses)) * 4;
		long estimatedUncachedTime = totalTimeMs + savedTime;
		return (double) estimatedUncachedTime / totalTimeMs;
	}

	/**
	 * Format metrics as human-readable string.
	 */
	public String format() {
		StringBuilder sb = new StringBuilder();
		sb.append("Total time: ").append(totalTimeMs).append("ms\n");
		sb.append("Cache: ").append(cacheHits).append(" hits, ")
			.append(cacheMisses).append(" misses (")
			.append(String.format("%.1f%%", cacheHitRate() * 100))
			.append(" hit rate)\n");

		if (cacheHits > 0) {
			sb.append("Cache speedup: ")
				.append(String.format("%.1fx", cacheSpeedup()))
				.append("\n");
		}

		sb.append("Slowest stage: ").append(getSlowestStage())
			.append(" (").append(getSlowestStageTime()).append("ms)\n");

		sb.append("Stage breakdown:\n");
		stageTimings.forEach((stage, time) -> {
			double percentage = (time * 100.0) / totalTimeMs;
			sb.append("  ").append(stage).append(": ")
				.append(time).append("ms (")
				.append(String.format("%.1f%%", percentage))
				.append(")\n");
		});

		return sb.toString();
	}
}
