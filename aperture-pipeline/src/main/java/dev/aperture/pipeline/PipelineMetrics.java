package dev.aperture.pipeline;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PipelineMetrics {
	private final long totalExecutionNanos;
	private final Map<String, Long> stageExecutionNanos;
	private final int cacheHits, cacheMisses, stagesExecuted, stagesSkipped;

	public PipelineMetrics(long totalExecutionNanos, Map<String, Long> stageExecutionNanos,
		int cacheHits, int cacheMisses, int stagesExecuted, int stagesSkipped) {
		if (totalExecutionNanos < 0) throw new IllegalArgumentException("totalExecutionNanos cannot be negative");
		this.totalExecutionNanos = totalExecutionNanos;
		this.stageExecutionNanos = Map.copyOf(Objects.requireNonNull(stageExecutionNanos));
		this.cacheHits = cacheHits; this.cacheMisses = cacheMisses;
		this.stagesExecuted = stagesExecuted; this.stagesSkipped = stagesSkipped;
	}
	public Duration totalExecutionTime() { return Duration.ofNanos(totalExecutionNanos); }
	public long totalExecutionNanos() { return totalExecutionNanos; }
	public Map<String, Long> stageExecutionNanos() { return stageExecutionNanos; }
	public Duration stageTime(String stage) { return Duration.ofNanos(stageExecutionNanos.getOrDefault(stage, 0L)); }
	public int cacheHits() { return cacheHits; }
	public int cacheMisses() { return cacheMisses; }
	public int stagesExecuted() { return stagesExecuted; }
	public int stagesSkipped() { return stagesSkipped; }
	public int stageCount() { return stagesExecuted + stagesSkipped; }
	public double cacheHitRate() { int total=cacheHits+cacheMisses; return total==0?0.0:(double)cacheHits/total; }

	public static final class Builder {
		private long totalNanos; private final Map<String,Long> stageNanos=new LinkedHashMap<>();
		private int hits, misses, executed, skipped;
		public Builder totalTime(long nanos){totalNanos=nanos;return this;}
		public Builder stageTime(String stage,long nanos){stageNanos.put(stage,nanos);return this;}
		public Builder cacheHit(){hits++;return this;} public Builder cacheMiss(){misses++;return this;}
		public Builder stageExecuted(){executed++;return this;} public Builder stageSkipped(){skipped++;return this;}
		public PipelineMetrics build(){return new PipelineMetrics(totalNanos,stageNanos,hits,misses,executed,skipped);}
	}
}