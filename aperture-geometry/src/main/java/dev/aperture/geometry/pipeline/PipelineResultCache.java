package dev.aperture.geometry.pipeline;

import dev.aperture.core.parameter.ParameterSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for pipeline generation results to avoid redundant computation.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class PipelineResultCache {

    private final Map<CacheKey, PipelineResult> cache = new ConcurrentHashMap<>();
    private final long maxCacheSize;
    private volatile long hits = 0;
    private volatile long misses = 0;

    /**
     * Creates a cache with default size limit (1000 entries).
     */
    public PipelineResultCache() {
        this(1000);
    }

    /**
     * Creates a cache with specified size limit.
     *
     * @param maxCacheSize Maximum number of cached results
     */
    public PipelineResultCache(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Gets a cached result or computes it if not present.
     *
     * @param typeId Opening type identifier
     * @param parameters Parameter set
     * @param generator Function to generate result if cache miss
     * @return Cached or newly computed pipeline result
     */
    public PipelineResult getOrCompute(
        String typeId,
        ParameterSet parameters,
        PipelineGenerator generator
    ) {
        CacheKey key = new CacheKey(typeId, parameters);

        PipelineResult cached = cache.get(key);
        if (cached != null) {
            hits++;
            return cached;
        }

        misses++;
        PipelineResult result = generator.generate();

        // Simple size limit: clear cache if too large
        if (cache.size() >= maxCacheSize) {
            cache.clear();
        }

        cache.put(key, result);
        return result;
    }

    /**
     * Invalidates a specific cache entry.
     *
     * @param typeId Opening type identifier
     * @param parameters Parameter set
     */
    public void invalidate(String typeId, ParameterSet parameters) {
        cache.remove(new CacheKey(typeId, parameters));
    }

    /**
     * Invalidates all cache entries for a given opening type.
     *
     * @param typeId Opening type identifier
     */
    public void invalidateType(String typeId) {
        cache.entrySet().removeIf(entry -> entry.getKey().typeId.equals(typeId));
    }

    /**
     * Clears the entire cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Returns cache statistics.
     */
    public CacheStats stats() {
        long totalRequests = hits + misses;
        double hitRate = totalRequests > 0 ? (double) hits / totalRequests : 0.0;
        return new CacheStats(cache.size(), hits, misses, hitRate);
    }

    /**
     * Cache key combining type ID and parameters.
     */
    private record CacheKey(String typeId, ParameterSet parameters) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey other)) return false;
            return typeId.equals(other.typeId) && parameters.equals(other.parameters);
        }

        @Override
        public int hashCode() {
            return 31 * typeId.hashCode() + parameters.hashCode();
        }
    }

    /**
     * Cache statistics snapshot.
     */
    public record CacheStats(
        long size,
        long hits,
        long misses,
        double hitRate
    ) {
        @Override
        public String toString() {
            return String.format(
                "CacheStats{size=%d, hits=%d, misses=%d, hitRate=%.2f%%}",
                size, hits, misses, hitRate * 100
            );
        }
    }

    /**
     * Functional interface for generating pipeline results.
     */
    @FunctionalInterface
    public interface PipelineGenerator {
        PipelineResult generate();
    }
}
