package dev.aperture.kernel;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class GenerationMetrics {
	private final Duration totalTime;
	private final int cacheHits, cacheMisses;
	private final Map<String, Duration> stageTimings;
	public GenerationMetrics(Duration totalTime, int cacheHits, int cacheMisses, Map<String, Duration> stageTimings) {
		this.totalTime=Objects.requireNonNull(totalTime); this.cacheHits=cacheHits; this.cacheMisses=cacheMisses;
		this.stageTimings=Map.copyOf(Objects.requireNonNull(stageTimings));
	}
	public Duration totalTime(){return totalTime;}
	public int cacheHits(){return cacheHits;} public int cacheMisses(){return cacheMisses;}
	public Map<String,Duration> stageTimings(){return stageTimings;}
	public Duration stageTime(String stage){return stageTimings.getOrDefault(stage,Duration.ZERO);}
	public double cacheHitRate(){int total=cacheHits+cacheMisses;return total==0?0.0:(double)cacheHits/total;}
	public String getSlowestStage(){return stageTimings.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("unknown");}
	public Duration getSlowestStageTime(){return stageTimings.values().stream().max(Duration::compareTo).orElse(Duration.ZERO);}
}