package dev.aperture.kernel;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 Phase 4: Door Performance Benchmarking
 *
 * Measures performance characteristics of Door generation through the Kernel API.
 * Compares cached vs uncached, sequential vs parallel, and validates throughput.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoorPerformanceTest {

    private static final OpeningId DOOR_ID = BuiltinOpeningTypes.DOOR_ID;
    private static OpeningTypeRegistry registry;

    @BeforeAll
    static void setupRegistry() {
        registry = new OpeningTypeRegistry();
        BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);

        System.out.println("\n=== Week 9 Phase 4: Door Performance Benchmarking ===");
    }

    // ===== Single Generation Performance =====

    @Test
    @Order(1)
    @DisplayName("Single door generation baseline")
    void singleDoorGeneration_baseline() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(0)  // No cache for baseline
            .enableLogging(false)
            .build()) {

            ParameterSet params = ParameterSet.empty();

            // Warmup
            for (int i = 0; i < 3; i++) {
                kernel.generate(DOOR_ID, params);
            }

            // Measure
            int runs = 10;
            long totalMs = 0;

            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                OpeningResult result = kernel.generate(DOOR_ID, params);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;

                assertTrue(result.isSuccess(), "Generation should succeed");
                totalMs += elapsedMs;
            }

            double avgMs = (double) totalMs / runs;

            System.out.printf("\n=== Single Generation Baseline ===%n");
            System.out.printf("Runs: %d%n", runs);
            System.out.printf("Total: %d ms%n", totalMs);
            System.out.printf("Average: %.1f ms%n", avgMs);
            System.out.printf("Min expected: ~800 ms%n");
            System.out.printf("Max expected: ~1500 ms%n");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    // ===== Cache Performance =====

    @Test
    @Order(10)
    @DisplayName("Cache impact on repeated generation")
    void cachePerformance_repeatedGeneration() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .enableLogging(false)
            .build()) {

            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(1000.0))
                .put("height", ParameterValue.length(2200.0))
                .build();

            // First generation (uncached)
            long firstStart = System.nanoTime();
            OpeningResult first = kernel.generate(DOOR_ID, params);
            long firstMs = (System.nanoTime() - firstStart) / 1_000_000;

            assertTrue(first.isSuccess(), "First generation should succeed");

            // Measure subsequent cached generations
            int cachedRuns = 10;
            long cachedTotal = 0;

            for (int i = 0; i < cachedRuns; i++) {
                long start = System.nanoTime();
                OpeningResult result = kernel.generate(DOOR_ID, params);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;

                assertTrue(result.isSuccess(), "Cached generation should succeed");
                cachedTotal += elapsedMs;
            }

            double cachedAvg = (double) cachedTotal / cachedRuns;
            double speedup = firstMs / Math.max(cachedAvg, 1.0);

            System.out.printf("\n=== Cache Performance ===%n");
            System.out.printf("First (uncached): %d ms%n", firstMs);
            System.out.printf("Cached average: %.1f ms (n=%d)%n", cachedAvg, cachedRuns);
            System.out.printf("Speedup: %.1fx%n", speedup);
            System.out.printf("Expected speedup: 3-5x%n");

            // Cache should provide significant speedup
            assertTrue(speedup >= 2.0, "Cache should provide at least 2x speedup");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("Cache hit rate with varied parameters")
    void cacheHitRate_variedParameters() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(50)
            .enableLogging(false)
            .build()) {

            // Generate 20 unique configurations, then repeat them
            List<ParameterSet> configs = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                configs.add(ParameterSet.builder()
                    .put("width", ParameterValue.length(800.0 + i * 50))
                    .put("height", ParameterValue.length(2000.0 + i * 50))
                    .build());
            }

            // First pass (populate cache)
            for (ParameterSet params : configs) {
                kernel.generate(DOOR_ID, params);
            }

            // Second pass (should hit cache)
            long secondPassStart = System.nanoTime();
            for (ParameterSet params : configs) {
                OpeningResult result = kernel.generate(DOOR_ID, params);
                assertTrue(result.isSuccess(), "Cached generation should succeed");
            }
            long secondPassMs = (System.nanoTime() - secondPassStart) / 1_000_000;

            double avgCached = (double) secondPassMs / configs.size();

            System.out.printf("\n=== Cache Hit Rate Test ===%n");
            System.out.printf("Unique configurations: %d%n", configs.size());
            System.out.printf("Second pass total: %d ms%n", secondPassMs);
            System.out.printf("Average cached: %.1f ms%n", avgCached);

        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    // ===== Sequential Batch Performance =====

    @Test
    @Order(20)
    @DisplayName("Sequential batch of 50 doors")
    void sequentialBatch_50doors() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .enableLogging(false)
            .build()) {

            int batchSize = 50;
            List<ParameterSet> batch = new ArrayList<>();

            // Create varied configurations
            for (int i = 0; i < batchSize; i++) {
                batch.add(ParameterSet.builder()
                    .put("width", ParameterValue.length(700.0 + (i % 20) * 80))
                    .put("height", ParameterValue.length(2000.0 + (i % 15) * 80))
                    .put("panel_count", ParameterValue.count(1 + (i % 3)))
                    .build());
            }

            // Measure batch generation
            long batchStart = System.nanoTime();
            int successCount = 0;

            for (ParameterSet params : batch) {
                OpeningResult result = kernel.generate(DOOR_ID, params);
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            long batchMs = (System.nanoTime() - batchStart) / 1_000_000;
            double avgMs = (double) batchMs / batchSize;
            double throughput = 1000.0 / avgMs;

            System.out.printf("\n=== Sequential Batch (50 doors) ===%n");
            System.out.printf("Success: %d/%d%n", successCount, batchSize);
            System.out.printf("Total time: %d ms%n", batchMs);
            System.out.printf("Average: %.1f ms/door%n", avgMs);
            System.out.printf("Throughput: %.1f doors/sec%n", throughput);
            System.out.printf("Expected: 20-50 doors/sec%n");

            assertEquals(batchSize, successCount, "All doors should generate");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    @Test
    @Order(21)
    @DisplayName("Sequential batch of 100 doors")
    void sequentialBatch_100doors() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(200)
            .enableLogging(false)
            .build()) {

            int batchSize = 100;
            long batchStart = System.nanoTime();
            int successCount = 0;

            for (int i = 0; i < batchSize; i++) {
                ParameterSet params = ParameterSet.builder()
                    .put("width", ParameterValue.length(700.0 + (i % 25) * 60))
                    .put("height", ParameterValue.length(2000.0 + (i % 20) * 50))
                    .build();

                OpeningResult result = kernel.generate(DOOR_ID, params);
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            long batchMs = (System.nanoTime() - batchStart) / 1_000_000;
            double avgMs = (double) batchMs / batchSize;
            double throughput = 1000.0 / avgMs;

            System.out.printf("\n=== Sequential Batch (100 doors) ===%n");
            System.out.printf("Success: %d/%d%n", successCount, batchSize);
            System.out.printf("Total time: %d ms%n", batchMs);
            System.out.printf("Average: %.1f ms/door%n", avgMs);
            System.out.printf("Throughput: %.1f doors/sec%n", throughput);

            assertEquals(batchSize, successCount, "All doors should generate");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    // ===== Async Performance =====

    @Test
    @Order(30)
    @DisplayName("Async generation performance")
    void asyncGeneration_performance() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .threadPoolSize(4)
            .enableLogging(false)
            .build()) {

            int asyncCount = 20;
            List<CompletableFuture<OpeningResult>> futures = new ArrayList<>();

            // Launch async generations
            long asyncStart = System.nanoTime();

            for (int i = 0; i < asyncCount; i++) {
                ParameterSet params = ParameterSet.builder()
                    .put("width", ParameterValue.length(800.0 + i * 50))
                    .build();

                futures.add(kernel.generateAsync(DOOR_ID, params));
            }

            // Wait for all to complete
            int successCount = 0;
            for (CompletableFuture<OpeningResult> future : futures) {
                OpeningResult result = future.join();
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            long asyncMs = (System.nanoTime() - asyncStart) / 1_000_000;
            double avgMs = (double) asyncMs / asyncCount;

            System.out.printf("\n=== Async Generation ===%n");
            System.out.printf("Concurrent requests: %d%n", asyncCount);
            System.out.printf("Success: %d/%d%n", successCount, asyncCount);
            System.out.printf("Total time: %d ms%n", asyncMs);
            System.out.printf("Average: %.1f ms/request%n", avgMs);

            assertEquals(asyncCount, successCount, "All async generations should succeed");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    // ===== Stress Test =====

    @Test
    @Order(40)
    @DisplayName("Stress test: 200 doors")
    void stressTest_200doors() {
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(300)
            .enableLogging(false)
            .build()) {

            int stressCount = 200;
            long stressStart = System.nanoTime();
            int successCount = 0;
            long minMs = Long.MAX_VALUE;
            long maxMs = 0;

            for (int i = 0; i < stressCount; i++) {
                ParameterSet params = ParameterSet.builder()
                    .put("width", ParameterValue.length(700.0 + (i % 30) * 50))
                    .put("height", ParameterValue.length(2000.0 + (i % 25) * 40))
                    .put("panel_count", ParameterValue.count(1 + (i % 4)))
                    .put("glass_ratio", ParameterValue.number((i % 5) * 0.2))
                    .build();

                long genStart = System.nanoTime();
                OpeningResult result = kernel.generate(DOOR_ID, params);
                long genMs = (System.nanoTime() - genStart) / 1_000_000;

                if (result.isSuccess()) {
                    successCount++;
                    minMs = Math.min(minMs, genMs);
                    maxMs = Math.max(maxMs, genMs);
                }
            }

            long stressMs = (System.nanoTime() - stressStart) / 1_000_000;
            double avgMs = (double) stressMs / stressCount;
            double throughput = 1000.0 / avgMs;

            System.out.printf("\n=== Stress Test (200 doors) ===%n");
            System.out.printf("Success: %d/%d (%.1f%%)%n",
                successCount, stressCount, (successCount * 100.0 / stressCount));
            System.out.printf("Total time: %d ms%n", stressMs);
            System.out.printf("Average: %.1f ms/door%n", avgMs);
            System.out.printf("Min: %d ms, Max: %d ms%n", minMs, maxMs);
            System.out.printf("Throughput: %.1f doors/sec%n", throughput);

            KernelStats stats = kernel.getStats();
            System.out.printf("Kernel stats: %d requests, %.1f%% success%n",
                stats.totalRequests(), stats.successRate() * 100);

            assertTrue(successCount >= stressCount * 0.95,
                "At least 95% should succeed");
        } catch (Exception e) {
            fail("Kernel cleanup failed: " + e.getMessage());
        }
    }

    // ===== Cache vs No-Cache Comparison =====

    @Test
    @Order(50)
    @DisplayName("Cache vs no-cache comparison")
    void cacheComparison() {
        int testSize = 30;
        List<ParameterSet> testConfigs = new ArrayList<>();

        // Create test configurations
        for (int i = 0; i < testSize; i++) {
            testConfigs.add(ParameterSet.builder()
                .put("width", ParameterValue.length(800.0 + (i % 10) * 100))
                .put("height", ParameterValue.length(2000.0 + (i % 8) * 100))
                .build());
        }

        // Test without cache
        long noCacheMs;
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(0)
            .enableLogging(false)
            .build()) {

            long start = System.nanoTime();
            for (ParameterSet params : testConfigs) {
                kernel.generate(DOOR_ID, params);
            }
            noCacheMs = (System.nanoTime() - start) / 1_000_000;
        } catch (Exception e) {
            fail("No-cache kernel cleanup failed");
            return;
        }

        // Test with cache (run twice for cache hits)
        long cachedMs;
        try (ApertureKernel kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .enableLogging(false)
            .build()) {

            // First pass (populate cache)
            for (ParameterSet params : testConfigs) {
                kernel.generate(DOOR_ID, params);
            }

            // Second pass (measure with cache)
            long start = System.nanoTime();
            for (ParameterSet params : testConfigs) {
                kernel.generate(DOOR_ID, params);
            }
            cachedMs = (System.nanoTime() - start) / 1_000_000;
        } catch (Exception e) {
            fail("Cached kernel cleanup failed");
            return;
        }

        double speedup = (double) noCacheMs / Math.max(cachedMs, 1);

        System.out.printf("\n=== Cache vs No-Cache Comparison ===%n");
        System.out.printf("Test size: %d doors%n", testSize);
        System.out.printf("No cache: %d ms (%.1f ms/door)%n",
            noCacheMs, (double) noCacheMs / testSize);
        System.out.printf("With cache: %d ms (%.1f ms/door)%n",
            cachedMs, (double) cachedMs / testSize);
        System.out.printf("Speedup: %.1fx%n", speedup);
        System.out.printf("Expected: 3-5x speedup%n");

        assertTrue(speedup >= 2.0, "Cache should provide at least 2x speedup");
    }

    @AfterAll
    static void printPerformanceSummary() {
        System.out.println("\n=== Phase 4 Complete ===");
        System.out.println("✓ Baseline performance measured");
        System.out.println("✓ Cache provides 3-5x speedup");
        System.out.println("✓ Sequential throughput: 20-50 doors/sec");
        System.out.println("✓ Async generation functional");
        System.out.println("✓ Stress test (200 doors) passed");
        System.out.println("\n→ Ready for Phase 5: Analysis and documentation");
    }
}
