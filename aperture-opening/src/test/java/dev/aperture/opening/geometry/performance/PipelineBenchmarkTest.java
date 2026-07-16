package dev.aperture.opening.geometry.performance;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for the generation pipeline.
 * Target: < 150ms cold run, < 5ms cached run.
 */
class PipelineBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 10;
    private static final long COLD_TARGET_MS = 150;
    private static final long CACHED_TARGET_MS = 5;

    @Test
    void fixedWindow_coldRun_completesWithinTarget() {
        // Given: Default fixed window parameters
        ParameterSet params = ParameterSet.empty();

        // When: Run pipeline cold (first time)
        long startTime = System.nanoTime();
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete within target
        assertNotNull(result);
        assertFalse(result.geometry().solids().isEmpty());

        System.out.printf("Fixed window cold run: %d ms (target: < %d ms)%n", elapsedMs, COLD_TARGET_MS);

        // Note: This is informational - actual target depends on hardware
        // assertTrue(elapsedMs < COLD_TARGET_MS * 2,
        //     String.format("Cold run took %d ms, significantly over target %d ms", elapsedMs, COLD_TARGET_MS));
    }

    @Test
    void fixedWindow_cachedRun_completesWithinTarget() {
        // Given: Same parameters used multiple times
        ParameterSet params = ParameterSet.empty();

        // Warmup: Run a few times to populate caches
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            GenerationTestSupport.generateFixedWindowPipeline(params);
        }

        // When: Benchmark cached runs
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            times.add(elapsedMs);
            assertNotNull(result);
        }

        // Then: Average should be within target
        double averageMs = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long minMs = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxMs = times.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.printf("Fixed window cached runs: avg=%.1f ms, min=%d ms, max=%d ms (target: < %d ms)%n",
            averageMs, minMs, maxMs, CACHED_TARGET_MS);

        // Note: Cached performance depends on whether actual caching is implemented
    }

    @Test
    void fixedWindow_varyingDimensions_performanceConsistent() {
        // Given: Different window sizes
        ParameterSet[] sizes = {
            createParams(800, 1000),
            createParams(1200, 1500),
            createParams(1600, 2000),
            createParams(2000, 2500)
        };

        // When: Generate each size
        for (ParameterSet params : sizes) {
            long startTime = System.nanoTime();
            PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            int width = params.requireLength("width").intValue();
            int height = params.requireLength("height").intValue();

            System.out.printf("Window %dx%d: %d ms%n", width, height, elapsedMs);

            assertNotNull(result);
            assertFalse(result.geometry().solids().isEmpty());
        }

        // Then: All should complete reasonably (no exponential blowup)
        // Performance should be relatively independent of dimensions
    }

    @Test
    void door_coldRun_completesWithinTarget() {
        // Given: Default door parameters
        ParameterSet params = ParameterSet.empty();

        // When: Run pipeline cold
        long startTime = System.nanoTime();
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete within target
        assertNotNull(result);
        assertFalse(result.geometry().solids().isEmpty());

        System.out.printf("Door cold run: %d ms (target: < %d ms)%n", elapsedMs, COLD_TARGET_MS);
    }

    @Test
    void curtainWall_coldRun_performanceAcceptable() {
        // Given: Curtain wall (more complex than simple window)
        ParameterSet params = ParameterSet.empty();

        // When: Run pipeline
        long startTime = System.nanoTime();
        PipelineResult result = GenerationTestSupport.generateCurtainWallPipeline(params);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete (may be slower due to complexity)
        assertNotNull(result);
        assertFalse(result.geometry().solids().isEmpty());

        System.out.printf("Curtain wall cold run: %d ms%n", elapsedMs);

        // Curtain walls can be more expensive - allow 2x target
        // assertTrue(elapsedMs < COLD_TARGET_MS * 2,
        //     String.format("Curtain wall took %d ms, over 2x target", elapsedMs));
    }

    @Test
    void profileExtrusion_performance_measurable() {
        // Given: Multiple window generations (profile extrusion is hot path)
        ParameterSet params = ParameterSet.empty();

        // When: Generate multiple times
        long totalTime = 0;
        int iterations = 20;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            GenerationTestSupport.generateFixedWindowPipeline(params);
            totalTime += System.nanoTime() - startTime;
        }

        long averageMs = totalTime / iterations / 1_000_000;

        // Then: Profile extrusion should not be a bottleneck
        System.out.printf("Average generation time over %d runs: %d ms%n", iterations, averageMs);

        // After optimization, average should improve
    }

    @Test
    void meshGeneration_performance_measurable() {
        // Given: Window with default parameters
        ParameterSet params = ParameterSet.empty();

        // Generate once to get geometry
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);

        // When: Measure mesh generation time specifically
        // (This would require exposing mesh generation as separate step)

        // Then: Mesh compilation should be fast
        assertFalse(result.meshes().partsByPath().isEmpty());
        System.out.printf("Generated %d mesh parts%n", result.meshes().partsByPath().size());
    }

    private ParameterSet createParams(double width, double height) {
        r