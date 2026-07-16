package dev.aperture.pipeline;

import dev.aperture.pipeline.stage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for pipeline execution.
 * <p>
 * These tests validate performance characteristics but are disabled by default
 * to avoid slowing down regular test runs. Enable with @Disabled removed when
 * profiling or validating performance improvements.
 */
@DisplayName("Pipeline Performance")
class PipelinePerformanceTest {

	private Pipeline pipeline;
	private PipelineCache cache;

	@BeforeEach
	void setUp() {
		cache = new PipelineCache(1000);

		pipeline = Pipeline.builder()
			.addStage(new DefinitionStage(PipelineTestFactory.registry()))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage())
			.addStage(PipelineTestFactory.componentStage())
			.addStage(PipelineTestFactory.geometryStage())
			.addStage(PipelineTestFactory.meshStage())
			.addStage(new CollisionStage())
			.addStage(new PlacementStage())
			.withCache(cache)
			.build();
	}

	@Test
	@DisplayName("Single execution completes within time budget")
	void testSingleExecutionPerformance() {
		// Arrange
		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());
		long timeoutMs = 5000; // 5 second budget

		// Act
		long startTime = System.currentTimeMillis();
		PipelineResult result = pipeline.execute(input);
		long elapsedTime = System.currentTimeMillis() - startTime;

		// Assert
		assertTrue(result.isSuccess(), "Pipeline should complete successfully");
		assertTrue(
			elapsedTime < timeoutMs,
			"Execution should complete within " + timeoutMs + "ms (took " + elapsedTime + "ms)"
		);

		System.out.println("Single execution time: " + elapsedTime + "ms");
	}

	@Test
	@DisplayName("Cached execution is significantly faster")
	void testCachingSpeedup() {
		// Arrange
		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());

		// Act - First execution (cold)
		long coldStart = System.currentTimeMillis();
		PipelineResult coldResult = pipeline.execute(input);
		long coldTime = System.currentTimeMillis() - coldStart;

		// Act - Second execution (warm)
		long warmStart = System.currentTimeMillis();
		PipelineResult warmResult = pipeline.execute(input);
		long warmTime = System.currentTimeMillis() - warmStart;

		// Assert
		assertTrue(coldResult.isSuccess());
		assertTrue(warmResult.isSuccess());

		double speedup = (double) coldTime / warmTime;
		assertTrue(
			speedup >= 2.0,
			"Cached execution should be at least 2x faster (speedup: " + String.format("%.1f", speedup) + "x)"
		);

		System.out.println("Cold execution: " + coldTime + "ms");
		System.out.println("Warm execution: " + warmTime + "ms");
		System.out.println("Speedup: " + String.format("%.1fx", speedup));
	}

	@Test
	@DisplayName("Batch execution throughput")
	void testBatchThroughput() {
		// Arrange
		int batchSize = 100;
		List<DefinitionStage.OpeningRequest> inputs = new ArrayList<>();
		for (int i = 0; i < batchSize; i++) {
			inputs.add(new DefinitionStage.OpeningRequest(
				"aperture:door_standard",
				Map.of("width", 1.0 + (i * 0.1))
			));
		}

		// Act
		long startTime = System.currentTimeMillis();
		List<PipelineResult> results = new ArrayList<>();
		for (var input : inputs) {
			results.add(pipeline.execute(input));
		}
		long elapsedTime = System.currentTimeMillis() - startTime;

		// Assert
		long successCount = results.stream().filter(PipelineResult::isSuccess).count();
		assertEquals(batchSize, successCount, "All executions should succeed");

		double throughput = (batchSize * 1000.0) / elapsedTime; // executions per second
		System.out.println("Batch size: " + batchSize);
		System.out.println("Total time: " + elapsedTime + "ms");
		System.out.println("Throughput: " + String.format("%.1f", throughput) + " executions/sec");
		System.out.println("Avg time per execution: " + String.format("%.1f", (double) elapsedTime / batchSize) + "ms");

		assertTrue(
			throughput > 10,
			"Should process at least 10 executions/sec (got " + String.format("%.1f", throughput) + ")"
		);
	}

	@Test
	@DisplayName("Cache hit rate for repeated patterns")
	void testCacheHitRate() {
		// Arrange - Create 10 distinct inputs
		List<DefinitionStage.OpeningRequest> inputs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			inputs.add(new DefinitionStage.OpeningRequest(
				"aperture:door_standard",
				Map.of("width", (double) i)
			));
		}

		// Act - Execute each input 10 times (100 total executions)
		cache.resetStatistics();
		for (int repeat = 0; repeat < 10; repeat++) {
			for (var input : inputs) {
				pipeline.execute(input);
			}
		}

		// Assert
		double hitRate = cache.getHitRate();
		System.out.println("Cache hit rate: " + String.format("%.1f%%", hitRate * 100));
		System.out.println("Cache hits: " + cache.getHits());
		System.out.println("Cache misses: " + cache.getMisses());

		assertTrue(
			hitRate > 0.85,
			"Cache hit rate should exceed 85% for repeated patterns (got " + String.format("%.1f%%", hitRate * 100) + ")"
		);
	}

	@Test
	@DisplayName("Memory usage remains stable")
	void testMemoryStability() {
		// Arrange
		Runtime runtime = Runtime.getRuntime();
		int iterations = 1000;

		// Force GC before measurement
		System.gc();
		Thread.yield();

		long initialMemory = runtime.totalMemory() - runtime.freeMemory();

		// Act - Execute many times
		for (int i = 0; i < iterations; i++) {
			pipeline.execute(new DefinitionStage.OpeningRequest(
				"aperture:door_standard",
				Map.of("width", (double) (i % 100)) // Reuse 100 distinct values
			));
		}

		// Force GC after execution
		System.gc();
		Thread.yield();

		long finalMemory = runtime.totalMemory() - runtime.freeMemory();
		long memoryGrowth = finalMemory - initialMemory;

		// Assert
		double growthMB = memoryGrowth / (1024.0 * 1024.0);
		System.out.println("Initial memory: " + String.format("%.1f", initialMemory / (1024.0 * 1024.0)) + " MB");
		System.out.println("Final memory: " + String.format("%.1f", finalMemory / (1024.0 * 1024.0)) + " MB");
		System.out.println("Memory growth: " + String.format("%.1f", growthMB) + " MB");

		assertTrue(
			growthMB < 50,
			"Memory growth should be less than 50MB for " + iterations + " iterations (grew " + String.format("%.1f", growthMB) + " MB)"
		);
	}

	@Test
	@DisplayName("Stage execution time distribution")
	void testStageTimingDistribution() {
		// Arrange
		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());

		// Act
		PipelineResult result = pipeline.execute(input);

		// Assert
		assertTrue(result.isSuccess());

		PipelineMetrics metrics = result.getMetrics();
		String[] stages = {
			"definition", "parameter", "constraint", "component",
			"geometry", "mesh", "collision", "placement"
		};

		System.out.println("\nStage timing breakdown:");
		System.out.println("─────────────────────────────────");

		long totalTime = result.executionTimeMs();
		for (String stage : stages) {
			long stageTime = metrics.getStageTime(stage);
			double percentage = (stageTime * 100.0) / totalTime;
			System.out.printf("%-12s: %4dms (%5.1f%%)%n", stage, stageTime, percentage);
		}

		System.out.println("─────────────────────────────────");
		System.out.printf("%-12s: %4dms%n", "Total", totalTime);
	}

	@Test
	@Disabled("Enable for stress testing")
	@DisplayName("Stress test - sustained load")
	void testSustainedLoad() {
		// Arrange
		int duration = 60_000; // 1 minute
		List<PipelineResult> results = new ArrayList<>();

		// Act
		long startTime = System.currentTimeMillis();
		int executionCount = 0;

		while (System.currentTimeMillis() - startTime < duration) {
			var input = new DefinitionStage.OpeningRequest(
				"aperture:door_standard",
				Map.of("width", (double) (executionCount % 50))
			);
			results.add(pipeline.execute(input));
			executionCount++;
		}

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Assert
		long successCount = results.stream().filter(PipelineResult::isSuccess).count();
		double successRate = (successCount * 100.0) / executionCount;
		double avgThroughput = (executionCount * 1000.0) / elapsedTime;

		System.out.println("\nStress Test Results:");
		System.out.println("Duration: " + (elapsedTime / 1000) + " seconds");
		System.out.println("Executions: " + executionCount);
		System.out.println("Success rate: " + String.format("%.1f%%", successRate));
		System.out.println("Avg throughput: " + String.format("%.1f", avgThroughput) + " exec/sec");
		System.out.println("Cache hit rate: " + String.format("%.1f%%", cache.getHitRate() * 100));

		assertTrue(successRate > 99.0, "Success rate should exceed 99%");
		assertTrue(avgThroughput > 10, "Average throughput should exceed 10 exec/sec");
	}

	@Test
	@DisplayName("Parallel execution scalability")
	void testParallelScalability() throws InterruptedException {
		// Arrange
		int threadCount = 4;
		int executionsPerThread = 25;
		List<Thread> threads = new ArrayList<>();
		List<Long> threadTimes = new ArrayList<>();

		// Act - Execute in parallel
		long startTime = System.currentTimeMillis();

		for (int t = 0; t < threadCount; t++) {
			final int threadId = t;
			threads.add(new Thread(() -> {
				long threadStart = System.currentTimeMillis();

				for (int i = 0; i < executionsPerThread; i++) {
					pipeline.execute(new DefinitionStage.OpeningRequest(
						"aperture:door_standard",
						Map.of("width", (double) ((threadId * executionsPerThread) + i))
					));
				}

				synchronized (threadTimes) {
					threadTimes.add(System.currentTimeMillis() - threadStart);
				}
			}));
		}

		threads.forEach(Thread::start);
		for (Thread thread : threads) {
			thread.join();
		}

		long totalTime = System.currentTimeMillis() - startTime;

		// Assert
		int totalExecutions = threadCount * executionsPerThread;
		double throughput = (totalExecutions * 1000.0) / totalTime;

		System.out.println("\nParallel Execution Results:");
		System.out.println("Threads: " + threadCount);
		System.out.println("Executions per thread: " + executionsPerThread);
		System.out.println("Total executions: " + totalExecutions);
		System.out.println("Total time: " + totalTime + "ms");
		System.out.println("Throughput: " + String.format("%.1f", throughput) + " exec/sec");

		assertTrue(
			throughput > 20,
			"Parallel throughput should exceed 20 exec/sec with " + threadCount + " threads"
		);
	}
}
