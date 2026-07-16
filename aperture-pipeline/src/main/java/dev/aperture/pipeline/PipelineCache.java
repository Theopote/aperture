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

	/**
	 * Create cache with specified capacity.
	 *
	 * @param capacity Maximum number of cached entries
	 */
	public PipelineCache(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.capacity = capacity;
		// LinkedHashMap with access-order for LRU
		this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
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
	 * @return Cached output if present
	 */
	public synchronized Optional<Object> get(String stageName, Object input) {
		Objects.requireNonNull(stageName, "stageName cannot be null");
		Objects.requireNonNull(input, "input cannot be null");

		CacheKey key = CacheKey.of(stageName, input);
		return Optional.ofNullable(cache.get(key));
	}

	/**
	 * Store result in cache.
	 *
	 * @param stageName Stage name
	 * @param input Stage input
	 * @param output Stage output
	 */
	public synchronized void put(String stageName, Object input, Object output) {
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
	 * Get cache statistics.
	 */
	public synchronized CacheStats getStats() {
		// Simple stats - could be extended to track hits/misses
		return new CacheStats(size(), capacity);
	}

	/**
	 * Cache statistics.
	 */
	public record CacheStats(int currentSize, int capacity) {
		public double utilizationRate() {
			return capacity == 0 ? 0.0 : (double) currentSize / capacity;
		}
	}
}
