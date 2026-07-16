package dev.aperture.geometry.pipeline;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PipelineResultCache implementation.
 */
class PipelineResultCacheTest {

    private PipelineResultCache cache;
    private AtomicInteger generationCount;

    @BeforeEach
    void setUp() {
        cache = new PipelineResultCache();
        generationCount = new AtomicInteger(0);
    }

    @Test
    void getOrCompute_firstCall_generatesResult() {
        // Given: Empty cache
        ParameterSet params = ParameterSet.empty();

        // When: First call
        PipelineResult result = cache.getOrCompute("test:window", params, this::generateMockResult);

        // Then: Should generate
        assertNotNull(result);
        assertEquals(1, generationCount.get(), "Should generate once");
        assertEquals(0, cache.stats().hits(), "Should have 0 hits");
        assertEquals(1, cache.stats().misses(), "Should have 1 miss");
    }

    @Test
    void getOrCompute_secondCall_returnsCached() {
        // Given: Cache with one entry
        ParameterSet params = ParameterSet.empty();
        cache.getOrCompute("test:window", params, this::generateMockResult);

        // When: Second call with same parameters
        PipelineResult result = cache.getOrCompute("test:window", params, this::generateMockResult);

        // Then: Should return cached, not regenerate
        assertNotNull(result);
        assertEquals(1, generationCount.get(), "Should only generate once");
        assertEquals(1, cache.stats().hits(), "Should have 1 hit");
        assertEquals(1, cache.stats().misses(), "Should still have 1 miss");
    }

    @Test
    void getOrCompute_differentParameters_generatesNew() {
        // Given: Cache with one entry
        ParameterSet params1 = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .build();
        cache.getOrCompute("test:window", params1, this::generateMockResult);

        // When: Call with different parameters
        ParameterSet params2 = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .build();
        PipelineResult result = cache.getOrCompute("test:window", params2, this::generateMockResult);

        // Then: Should generate new result
        assertNotNull(result);
        assertEquals(2, generationCount.get(), "Should generate twice");
        assertEquals(0, cache.stats().hits(), "Should have 0 hits");
        assertEquals(2, cache.stats().misses(), "Should have 2 misses");
    }

    @Test
    void getOrCompute_differentTypeId_generatesNew() {
        // Given: Cache with window entry
        ParameterSet params = ParameterSet.empty();
        cache.getOrCompute("test:window", params, this::generateMockResult);

        // When: Call with different type ID
        PipelineResult result = cache.getOrCompute("test:door", params, this::generateMockResult);

        // Then: Should generate new result
        assertNotNull(result);
        assertEquals(2, generationCount.get(), "Should generate twice");
    }

    @Test
    void invalidate_removesSpecificEntry() {
        // Given: Cache with two entries
        ParameterSet params1 = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .build();
        ParameterSet params2 = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .build();
        cache.getOrCompute("test:window", params1, this::generateMockResult);
        cache.getOrCompute("test:window", params2, this::generateMockResult);

        // When: Invalidate one entry
        cache.invalidate("test:window", params1);

        // Then: First should miss, second should hit
        generationCount.set(0);
        cache.getOrCompute("test:window", params1, this::generateMockResult);
        assertEquals(1, generationCount.get(), "Should regenerate invalidated entry");

        cache.getOrCompute("test:window", params2, this::generateMockResult);
        assertEquals(1, generationCount.get(), "Should not regenerate other entry");
    }

    @Test
    void invalidateType_removesAllEntriesForType() {
        // Given: Cache with entries for different types
        ParameterSet params = ParameterSet.empty();
        cache.getOrCompute("test:window", params, this::generateMockResult);
        cache.getOrCompute("test:door", params, this::generateMockResult);

        // When: Invalidate window type
        cache.invalidateType("test:window");

        // Then: Window should miss, door should hit
        generationCount.set(0);
        cache.getOrCompute("test:window", params, this::generateMockResult);
        assertEquals(1, generationCount.get(), "Should regenerate window");

        cache.getOrCompute("test:door", params, this::generateMockResult);
        assertEquals(1, generationCount.get(), "Should not regenerate door");
    }

    @Test
    void clear_removesAllEntries() {
        // Given: Cache with multiple entries
        cache.getOrCompute("test:window", ParameterSet.empty(), this::generateMockResult);
        cache.getOrCompute("test:door", ParameterSet.empty(), this::generateMockResult);
        assertEquals(2, cache.stats().size());

        // When: Clear cache
        cache.clear();

        // Then: All entries should be removed
        assertEquals(0, cache.stats().size());
        assertEquals(0, cache.stats().hits());
        assertEquals(0, cache.stats().misses());
    }

    @Test
    void stats_tracksHitsAndMisses() {
        // Given: Empty cache
        ParameterSet params = ParameterSet.empty();

        // When: Mix of hits and misses
        cache.getOrCompute("test:window", params, this::generateMockResult); // miss
        cache.getOrCompute("test:window", params, this::generateMockResult); // hit
        cache.getOrCompute("test:window", params, this::generateMockResult); // hit

        ParameterSet params2 = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .build();
        cache.getOrCompute("test:window", params2, this::generateMockResult); // miss

        // Then: Stats should be accurate
        var stats = cache.stats();
        assertEquals(2, stats.hits(), "Should have 2 hits");
        assertEquals(2, stats.misses(), "Should have 2 misses");
        assertEquals(0.5, stats.hitRate(), 0.01, "Hit rate should be 50%");
        assertEquals(2, stats.size(), "Should have 2 entries");
    }

    @Test
    void cache_threadSafe_concurrentAccess() throws InterruptedException {
        // Given: Shared cache
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ParameterSet params = ParameterSet.empty();

        // When: Multiple threads access concurrently
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    cache.getOrCompute("test:window", params, this::generateMockResult);
                }
                latch.countDown();
            });
            threads[i].start();
        }

        latch.await();

        // Then: Should only generate once despite concurrent access
        assertTrue(generationCount.get() >= 1, "Should generate at least once");
        assertTrue(generationCount.get() <= threadCount,
            "Should not generate more than thread count due to race conditions");

        // Cache should be consistent
        assertEquals(1, cache.stats().size(), "Should have 1 entry");
    }

    @Test
    void cache_sizeLimited_clearsWhenFull() {
        // Given: Small cache
        PipelineResultCache smallCache = new PipelineResultCache(5);

        // When: Add more entries than limit
        for (int i = 0; i < 10; i++) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(1000.0 + i * 100))
                .build();
            smallCache.getOrCompute("test:window", params, this::generateMockResult);
        }

        // Then: Cache should have been cleared
        assertTrue(smallCache.stats().size() <= 5,
            "Cache size should not exceed limit significantly");
    }

    @Test
    void cacheKey_equality_sameParametersEqual() {
        // Given: Same parameters in different instances
        ParameterSet params1 = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .put("height", ParameterValue.length(1500.0))
            .build();
        ParameterSet params2 = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        // When: Use both parameter sets
        cache.getOrCompute("test:window", params1, this::generateMockResult);
        cache.getOrCompute("test:window", params2, this::generateMockResult);

        // Then: Should be cache hit (same key)
        assertEquals(1, generationCount.get(), "Should use same cache entry");
        assertEquals(1, cache.stats().hits(), "Should have 1 cache hit");
    }

    @Test
    void cacheStats_toString_formatsCorrectly() {
        // Given: Cache with some activity
        cache.getOrCompute("test:window", ParameterSet.empty(), this::generateMockResult);
        cache.getOrCompute("test:window", ParameterSet.empty(), this::generateMockResult);

        // When: Get stats string
        String statsStr = cache.stats().toString();

        // Then: Should contain key metrics
        assertTrue(statsStr.contains("size=1"), "Should show size");
        assertTrue(statsStr.contains("hits=1"), "Should show hits");
        assertTrue(statsStr.contains("misses=1"), "Should show misses");
        assertTrue(statsStr.contains("hitRate=50.00%"), "Should show hit rate");
    }

    private PipelineResult generateMockResult() {
        generationCount.incrementAndGet();
        return PipelineResult.empty();
    }
}
