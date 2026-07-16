package dev.aperture.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Thread-safe LRU cache addressed only by explicit structural stage keys. */
public final class PipelineCache {
	private final int capacity;
	private final Map<StageCacheKey, Object> cache;
	private long hits;
	private long misses;

	public PipelineCache(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity cannot be negative");
		}
		this.capacity = capacity;
		this.cache = new LinkedHashMap<>(capacity == 0 ? 1 : capacity, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<StageCacheKey, Object> eldest) {
				return size() > PipelineCache.this.capacity;
			}
		};
	}

	public synchronized Object get(StageCacheKey key) {
		Objects.requireNonNull(key, "key cannot be null");
		if (capacity == 0) {
			misses++;
			return null;
		}
		Object result = cache.get(key);
		if (result == null) {
			misses++;
		} else {
			hits++;
		}
		return result;
	}

	public synchronized void put(StageCacheKey key, Object output) {
		Objects.requireNonNull(key, "key cannot be null");
		Objects.requireNonNull(output, "output cannot be null");
		if (capacity > 0) {
			cache.put(key, output);
		}
	}

	public synchronized void invalidate(StageId stage) {
		Objects.requireNonNull(stage, "stage cannot be null");
		cache.keySet().removeIf(key -> key.stage() == stage);
	}

	public synchronized void clear() {
		cache.clear();
	}

	public synchronized int size() { return cache.size(); }
	public int capacity() { return capacity; }
	public synchronized long getHits() { return hits; }
	public synchronized long getMisses() { return misses; }

	public synchronized double getHitRate() {
		long total = hits + misses;
		return total == 0 ? 0.0 : (double) hits / total;
	}

	public synchronized void resetStatistics() {
		hits = 0;
		misses = 0;
	}

	public synchronized CacheStats getStats() {
		return new CacheStats(size(), capacity, hits, misses);
	}

	public record CacheStats(int currentSize, int capacity, long hits, long misses) {
		public double utilizationRate() {
			return capacity == 0 ? 0.0 : (double) currentSize / capacity;
		}

		public double hitRate() {
			long total = hits + misses;
			return total == 0 ? 0.0 : (double) hits / total;
		}
	}
}