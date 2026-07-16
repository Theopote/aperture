package dev.aperture.pipeline;

import java.util.Map;
import java.util.Objects;

/**
 * Performance metrics collected during pipeline execution.
 */
public record PipelineMetrics(
	long totalExecutionTimeMs,
	Map<String, Long> stageExecutionTimes,
	int cacheHits,
	int cacheMisses,
	int stagesExecuted,
	int stagesSkipped
) {
	public PipelineMetrics {
		Objects.requireNonNull(stageExecutionTimes, "stageExecutionTimes cannot be null");
		if (totalExecutionTimeMs < 0) {
			throw new IllegalArgumentException("totalExecutionTimeMs cannot be negative");
		}
		if (cacheHits < 0 || cacheMisses < 0) {
			throw new IllegalArgumentException("cache hits/misses cannot be negative");
		}
		if (stagesExecuted < 0 || stagesSkipped < 0) {
			throw new IllegalArgumentException("stage counts cannot be negative");
		}
	}

	/**
	 * Calculate cache hit rate (0.0 to 1.0).
	 */
	public double cacheHitRate() {
		int total = cacheHits + cacheMisses;
		return total == 0 ? 0.0 : (double) cacheHits / total;
	}

	/**
	 * Get execution time for a specific stage.
	 */
	public long getStageTime(String stageName) {
		Objects.requireNonNull(stageName, "stageName cannot be null");
		return stageExecutionTimes.getOrDefault(stageName, 0L);
	}

	/**
	 * Format metrics as a human-readable report.
	 */
	public String formatReport() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pipeline Execution Report:\n");
		sb.append(String.format("  Total Time: %dms\n", totalExecutionTimeMs));
		sb.append(String.format("  Stages Executed: %d\n", stagesExecuted));
		sb.append(String.format("  Stages Skipped: %d\n", stagesSkipped));
		sb.append(String.format("  Cache Hit Rate: %.1f%%\n", cacheHitRate() * 100));
		sb.append("  Stage Times:\n");

		stageExecutionTimes.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry ->
				sb.append(String.format("    %s: %dms\n", entry.getKey(), entry.getValue()))
			);

		return sb.toString();
	}

	/**
	 * Builder for constructing metrics incrementally.
	 */
	public static class Builder {
		private long totalTime = 0;
		private final Map<String, Long> stageTimes = new java.util.LinkedHashMap<>();
		private int cacheHits = 0;
		private int cacheMisses = 0;
		private int stagesExecuted = 0;
		private int stagesSkipped = 0;

		public Builder totalTime(long ms) {
			this.totalTime = ms;
			return this;
		}

		public Builder stageTime(String stageName, long ms) {
			stageTimes.put(stageName, ms);
			return this;
		}

		public Builder cacheHit() {
			this.cacheHits++;
			return this;
		}

		public Builder cacheMiss() {
			this.cacheMisses++;
			return this;
		}

		public Builder stageExecuted() {
			this.stagesExecuted++;
			return this;
		}

		public Builder stageSkipped() {
			this.stagesSkipped++;
			return this;
		}

		public PipelineMetrics build() {
			return new PipelineMetrics(
				totalTime,
				Map.copyOf(stageTimes),
				cacheHits,
				cacheMisses,
				stagesExecuted,
				stagesSkipped
			);
		}
	}
}
