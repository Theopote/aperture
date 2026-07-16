package dev.aperture.kernel;

import dev.aperture.pipeline.PipelineCache;
import java.util.Map;
import java.util.Objects;

/**
 * Kernel-level statistics.
 * <p>
 * Aggregates statistics across all opening generation requests.
 */
public record KernelStats(
	long totalRequests,
	long successfulRequests,
	long failedRequests,
	double averageExecutionTimeMs,
	PipelineCache.CacheStats cacheStats,
	Map<String, Long> failuresByStage
) {
	public KernelStats {
		Objects.requireNonNull(cacheStats, "cacheStats cannot be null");
		Objects.requireNonNull(failuresByStage, "failuresByStage cannot be null");
		failuresByStage = Map.copyOf(failuresByStage); // Immutable
	}

	/**
	 * Calculate success rate (0.0 to 1.0).
	 */
	public double successRate() {
		return totalRequests == 0 ? 0.0 :
			(double) successfulRequests / totalRequests;
	}

	/**
	 * Calculate failure rate (0.0 to 1.0).
	 */
	public double failureRate() {
		return totalRequests == 0 ? 0.0 :
			(double) failedRequests / totalRequests;
	}

	/**
	 * Get number of failures for a specific stage.
	 */
	public long getFailureCount(String stageName) {
		return failuresByStage.getOrDefault(stageName, 0L);
	}

	/**
	 * Get the stage with most failures.
	 */
	public String getMostFailedStage() {
		return failuresByStage.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse("none");
	}

	/**
	 * Check if kernel is performing well.
	 * <p>
	 * Returns true if success rate > 95% and cache hit rate > 70%.
	 */
	public boolean isHealthy() {
		return successRate() > 0.95 && cacheStats.hitRate() > 0.70;
	}

	/**
	 * Format statistics as human-readable string.
	 */
	public String format() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== Kernel Statistics ===\n");
		sb.append("Total requests: ").append(totalRequests).append("\n");
		sb.append("Successful: ").append(successfulRequests)
			.append(" (").append(String.format("%.1f%%", successRate() * 100))
			.append(")\n");
		sb.append("Failed: ").append(failedRequests)
			.append(" (").append(String.format("%.1f%%", failureRate() * 100))
			.append(")\n");
		sb.append("Avg execution time: ")
			.append(String.format("%.1fms", averageExecutionTimeMs))
			.append("\n");

		sb.append("\n=== Cache Statistics ===\n");
		sb.append("Size: ").append(cacheStats.currentSize())
			.append(" / ").append(cacheStats.capacity()).append("\n");
		sb.append("Hits: ").append(cacheStats.hits()).append("\n");
		sb.append("Misses: ").append(cacheStats.misses()).append("\n");
		sb.append("Hit rate: ")
			.append(String.format("%.1f%%", cacheStats.hitRate() * 100))
			.append("\n");

		if (!failuresByStage.isEmpty()) {
			sb.append("\n=== Failures by Stage ===\n");
			failuresByStage.forEach((stage, count) -> {
				sb.append("  ").append(stage).append(": ").append(count).append("\n");
			});
			sb.append("Most failed stage: ").append(getMostFailedStage()).append("\n");
		}

		sb.append("\nHealth status: ").append(isHealthy() ? "✓ Healthy" : "✗ Degraded");

		return sb.toString();
	}
}
