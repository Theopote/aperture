package dev.aperture.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Cache for pipeline stage results.
 * <p>
 * Uses LRU (Least Recently Used) eviction policy when capacity is exceeded.
 * Cache keys are based on stage name and input hash.
 */
public final class PipelineCache {
	private final int capacity;
	private final Map<CacheKey, Object> cache;
	private long hits = 0;
	private long misses = 0;

	/**
	 * Create cache with specified capacity.
	 *
	 * @param capacity Maximum number of cached entries (0 to disable caching)
	 */
	public PipelineCache(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity cannot be negative");
		}
		this.capacity = capacity;
		// LinkedHashMap with access-order for LRU
		this.cache = new LinkedHashMap<>(capacity == 0 ? 1 : capacity, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<CacheKey, Object> eldest) {
				return size() > PipelineCache.this.capacity;
			}
		};
	}

	/**
	 * Get cached result for stage and input.
	 *
	 * @param stageName Stage name
	 * @param input Stage input
	 * @return Cached output if present, or null if not cached
	 */
	public synchronized Object get(String stageName, Object input) {
		if (capacity == 0) {
			misses++;
			return null;
		}

		CacheKey key = CacheKey.of(stageName, input);
		Object result = cache.get(key);

		if (result != null) {
			hits++;
		} else {
			misses++;
		}

		return result;
	}

	/**
	 * Store result in cache.
	 *
	 * @param stageName Stage name
	 * @param input Stage input
	 * @param output Stage output
	 */
	public synchronized void put(String stageName, Object input, Object output) {
		if (capacity == 0) {
			return; // Caching disabled
		}

		Objects.requireNonNull(stageName, "stageName cannot be null");
		Objects.requireNonNull(input, "input cannot be null");
		Objects.requireNonNull(output, "output cannot be null");

		CacheKey key = CacheKey.of(stageName, input);
		cache.put(key, output);
	}

	/**
	 * Invalidate all cached results for a specific stage.
	 *
	 * @param stageName Stage name
	 */
	public synchronized void invalidate(String stageName) {
		Objects.requireNonNull(stageName, "stageName cannot be null");
		cache.keySet().removeIf(key -> key.stageName().equals(stageName));
	}

	/**
	 * Clear all cached results.
	 */
	public synchronized void clear() {
		cache.clear();
	}

	/**
	 * Get current cache size.
	 */
	public synchronized int size() {
		return cache.size();
	}

	/**
	 * Get cache capacity.
	 */
	public int capacity() {
		return capacity;
	}

	/**
	 * Cache key combining stage name and input hash.
	 */
	record CacheKey(String stageName, int inputHash) {
		CacheKey {
			Objects.requireNonNull(stageName, "stageName cannot be null");
		}

		static CacheKey of(String stageName, Object input) {
			return new CacheKey(stageName, computeHash(input));
		}

		private static int computeHash(Object input) {
			// Deep hash for content-based caching
			// For now, use standard hashCode
			// TODO: Consider implementing deep structural hashing
			return Objects.hashCode(input);
		}
	}

	/**
	 * Get number of cache hits.
	 */
	public synchronized long getHits() {
		return hits;
	}

	/**
	 * Get number of cache misses.
	 */
	public synchronized long getMisses() {
		return misses;
	}

	/**
	 * Get cache hit rate (hits / total accesses).
	 */
	public synchronized double getHitRate() {
		long total = hits + misses;
		return total == 0 ? 0.0 : (double) hits / total;
	}

	/**
	 * Reset hit/miss statistics.
	 */
	public synchronized void resetStatistics() {
		hits = 0;
		misses = 0;
	}

	/**
	 * Get cache statistics.
	 */
	public synchronized CacheStats getStats() {
		return new CacheStats(size(), capacity, hits, misses);
	}

	/**
	 * Cache statistics.
	 */
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
