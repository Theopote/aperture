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
 * Comprehensive performance benchmark tests for pipeline operations.
 * Targets: Cold generation < 150ms, Cached generation < 5ms
 */
class ComprehensiveBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 20;
    private static final long COLD_TARGET_MS = 150;
    private static final long CACHED_TARGET_MS = 5;

    @Test
    void benchmark_fixedWindow_coldGeneration() {
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        // Warmup JIT
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            GenerationTestSupport.generateFixedWindowPipeline(params);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);

            assertNotNull(result);
        }

        BenchmarkStats stats = BenchmarkStats.compute(times);
        stats.print("Fixed Window Cold Generation");

        // Informational: should be under target
        if (stats.median > COLD_TARGET_MS) {
            System.out.printf("⚠ Warning: Median time %.1fms exceeds target %dms%n",
                stats.median, COLD_TARGET_MS);
        }
    }

    @Test
    void benchmark_door_coldGeneration() {
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("panel_count", ParameterValue.count(1))
            .build();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            GenerationTestSupport.generateDoorPipeline(params);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);

            assertNotNull(result);
        }

        BenchmarkStats stats = BenchmarkStats.compute(times);
        stats.print("Door Cold Generation");

        if (stats.median > COLD_TARGET_MS) {
            System.out.printf("⚠ Warning: Median time %.1fms exceeds target %dms%n",
                stats.median, COLD_TARGET_MS);
        }
    }

    @Test
    void benchmark_curtainWall_coldGeneration() {
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(3000.0))
            .put("height", ParameterValue.length(2700.0))
            .put("horizontal_divisions", ParameterValue.count(2))
            .put("vertical_divisions", ParameterValue.count(1))
            .build();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            GenerationTestSupport.generateCurtainWallPipeline(params);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            PipelineResult result = GenerationTestSupport.generateCurtainWallPipeline(params);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);

            assertNotNull(result);
        }

        BenchmarkStats stats = BenchmarkStats.compute(times);
        stats.print("Curtain Wall Cold Generation");

        // Curtain walls are more complex, allow higher target
        long curtainWallTarget = COLD_TARGET_MS * 2; // 300ms
        if (stats.median > curtainWallTarget) {
            System.out.printf("⚠ Warning: Median time %.1fms exceeds target %dms%n",
                stats.median, curtainWallTarget);
        }
    }

    @Test
    void benchmark_varyingComplexity() {
        System.out.println("\n=== Varying Complexity Benchmark ===");

        // Small window
        benchmarkCase("Small Window (600x800)",
            ParameterSet.builder()
                .put("width", ParameterValue.length(600.0))
                .put("height", ParameterValue.length(800.0))
                .build(),
            GenerationType.FIXED_WINDOW);

        // Medium window
        benchmarkCase("Medium Window (1200x1500)",
            ParameterSet.builder()
                .put("width", ParameterValue.length(1200.0))
                .put("height", ParameterValue.length(1500.0))
                .build(),
            GenerationType.FIXED_WINDOW);

        // Large window
        benchmarkCase("Large Window (2000x2500)",
            ParameterSet.builder()
                .put("width", ParameterValue.length(2000.0))
                .put("height", ParameterValue.length(2500.0))
                .build(),
            GenerationType.FIXED_WINDOW);

        // Single door
        benchmarkCase("Single Door",
            ParameterSet.builder()
                .put("width", ParameterValue.length(900.0))
                .put("height", ParameterValue.length(2100.0))
                .put("panel_count", ParameterValue.count(1))
                .build(),
            GenerationType.DOOR);

        // Double door
        benchmarkCase("Double Door",
            ParameterSet.builder()
                .put("width", ParameterValue.length(1800.0))
                .put("height", ParameterValue.length(2300.0))
                .put("panel_count", ParameterValue.count(2))
                .build(),
            GenerationType.DOOR);
    }

    @Test
    void benchmark_parallelGeneration() {
        System.out.println("\n=== Parallel Generation Benchmark ===");

        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        int parallelCount = Runtime.getRuntime().availableProcessors();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            GenerationTestSupport.generateFixedWindowPipeline(params);
        }

        // Sequential benchmark
        long seqStart = System.nanoTime();
        for (int i = 0; i < parallelCount; i++) {
            GenerationTestSupport.generateFixedWindowPipeline(params);
        }
        long seqTime = (System.nanoTime() - seqStart) / 1_000_000;

        // Parallel benchmark
        long parStart = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < parallelCount; i++) {
            Thread t = new Thread(() -> {
                GenerationTestSupport.generateFixedWindowPipeline(params);
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long parTime = (System.nanoTime() - parStart) / 1_000_000;

        double speedup = (double) seqTime / parTime;
        System.out.printf("Sequential time: %dms%n", seqTime);
        System.out.printf("Parallel time: %dms%n", parTime);
        System.out.printf("Speedup: %.2fx with %d threads%n", speedup, parallelCount);
    }

    @Test
    void benchmark_memoryUsage() {
        System.out.println("\n=== Memory Usage Benchmark ===");

        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        // Force GC
        System.gc();
        System.gc();
        Thread.yield();

        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        // Generate multiple instances
        int instanceCount = 100;
        List<PipelineResult> results = new ArrayList<>();

        long genStart = System.nanoTime();
        for (int i = 0; i < instanceCount; i++) {
            results.add(GenerationTestSupport.generateFixedWindowPipeline(params));
        }
        long genTime = (System.nanoTime() - genStart) / 1_000_000;

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemory = afterMemory - beforeMemory;
        long avgMemoryPerInstance = usedMemory / instanceCount;

        System.out.printf("Generated %d instances in %dms%n", instanceCount, genTime);
        System.out.printf("Total memory used: %.2f MB%n", usedMemory / (1024.0 * 1024.0));
        System.out.printf("Average per instance: %.2f KB%n", avgMemoryPerInstance / 1024.0);
        System.out.printf("Average generation time: %.1fms%n", (double) genTime / instanceCount);

        // Keep results alive
        assertFalse(results.isEmpty());
    }

    @Test
    void benchmark_meshComplexity() {
        System.out.println("\n=== Mesh Complexity Analysis ===");

        analyzeComplexity("Fixed Window 1200x1500",
            GenerationTestSupport.generateFixedWindowPipeline(ParameterSet.builder()
                .put("width", ParameterValue.length(1200.0))
                .put("height", ParameterValue.length(1500.0))
                .build()));

        analyzeComplexity("Door Single Panel",
            GenerationTestSupport.generateDoorPipeline(ParameterSet.builder()
                .put("width", ParameterValue.length(900.0))
                .put("height", ParameterValue.length(2100.0))
                .put("panel_count", ParameterValue.count(1))
                .build()));

        analyzeComplexity("Door Double Panel",
            GenerationTestSupport.generateDoorPipeline(ParameterSet.builder()
                .put("width", ParameterValue.length(1800.0))
                .put("height", ParameterValue.length(2300.0))
                .put("panel_count", ParameterValue.count(2))
                .build()));

        analyzeComplexity("Curtain Wall",
            GenerationTestSupport.generateCurtainWallPipeline(ParameterSet.empty()));
    }

    private void analyzeComplexity(String name, PipelineResult result) {
        int totalVertices = 0;
        int totalFaces = 0;
        int partCount = result.meshes().partsByPath().size();

        for (var mesh : result.meshes().partsByPath().values()) {
            totalVertices += mesh.vertexCount();
            totalFaces += mesh.faceCount();
        }

        System.out.printf("%s:%n", name);
        System.out.printf("  Parts: %d%n", partCount);
        System.out.printf("  Total Vertices: %d%n", totalVertices);
        System.out.printf("  Total Faces: %d%n", totalFaces);
        System.out.printf("  Avg Vertices/Part: %.1f%n", (double) totalVertices / partCount);
        System.out.printf("  Avg Faces/Part: %.1f%n", (double) totalFaces / partCount);
        System.out.println();
    }

    private void benchmarkCase(String name, ParameterSet params, GenerationType type) {
        // Warmup
        for (int i = 0; i < 3; i++) {
            generate(type, params);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            generate(type, params);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            times.add(elapsed);
        }

        BenchmarkStats stats = BenchmarkStats.compute(times);
        System.out.printf("%s: median=%.1fms, p95=%.1fms%n",
            name, stats.median, stats.p95);
    }

    private PipelineResult generate(GenerationType type, ParameterSet params) {
        return switch (type) {
            case FIXED_WINDOW -> GenerationTestSupport.generateFixedWindowPipeline(params);
            case DOOR -> GenerationTestSupport.generateDoorPipeline(params);
            case CURTAIN_WALL -> GenerationTestSupport.generateCurtainWallPipeline(params);
        };
    }

    private enum GenerationType {
        FIXED_WINDOW,
        DOOR,
        CURTAIN_WALL
    }

    private static class BenchmarkStats {
        final double median;
        final double mean;
        final double min;
        final double max;
        final double p95;
        final double stdDev;

        BenchmarkStats(double median, double mean, double min, double max, double p95, double stdDev) {
            this.median = median;
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.p95 = p95;
            this.stdDev = stdDev;
        }

        static BenchmarkStats compute(List<Long> times) {
            times.sort(Long::compareTo);

            double median = times.get(times.size() / 2);
            double mean = times.stream().mapToLong(Long::longValue).average().orElse(0);
            double min = times.get(0);
            double max = times.get(times.size() - 1);
            double p95 = times.get((int) (times.size() * 0.95));

            double variance = times.stream()
                .mapToDouble(t -> Math.pow(t - mean, 2))
                .average()
                .orElse(0);
            double stdDev = Math.sqrt(variance);

            return new BenchmarkStats(median, mean, min, max, p95, stdDev);
        }

        void print(String name) {
            System.out.printf("\n===