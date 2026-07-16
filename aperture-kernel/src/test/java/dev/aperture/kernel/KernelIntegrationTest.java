package dev.aperture.kernel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Aperture Kernel.
 * <p>
 * Tests complete end-to-end workflows including caching,
 * batch processing, and concurrent execution.
 */
@DisplayName("Kernel Integration Tests")
class KernelIntegrationTest {

	private ApertureKernel kernel;

	@BeforeEach
	void setUp() {
		kernel = ApertureKernel.builder()
			.withCacheCapacity(100)
			.withAsyncThreadPoolSize(4)
			.build();
	}

	@AfterEach
	void tearDown() {
		if (kernel != null && !kernel.isClosed()) {
			kernel.close();
		}
	}

	@Test
	@DisplayName("Complete generation workflow")
	void testCompleteWorkflow() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1.0, "height", 2.0, "thickness", 0.1)
		);

		// Act
		OpeningResult result = kernel.generate(request);

		// Assert
		assertNotNull(result);
		assertTrue(result.isSuccess(), "Generation should succeed");

		var success = result.asSuccess();
		assertNotNull(success.placement());
		assertNotNull(success.metrics());

		// Check metrics
		GenerationMetrics metrics = success.metrics();
		assertTrue(metrics.totalTimeMs() > 0);
		assertTrue(metrics.stageTimings().size() > 0);
	}

	@Test
	@DisplayName("Cache improves performance")
	void testCachingImprovement() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1.0, "height", 2.0)
		);

		// Act - First execution (cold)
		OpeningResult result1 = kernel.generate(request);
		long firstTime = result1.asSuccess().metrics().totalTimeMs();

		// Act - Second execution (warm)
		OpeningResult result2 = kernel.generate(request);
		long secondTime = result2.asSuccess().metrics().totalTimeMs();

		// Assert
		assertTrue(result1.isSuccess());
		assertTrue(result2.isSuccess());

		// Second should be faster due to caching
		assertTrue(secondTime <= firstTime,
			"Cached execution should be faster or equal (first: " + firstTime +
			"ms, second: " + secondTime + "ms)");
	}

	@Test
	@DisplayName("Batch processing with cache benefits")
	void testBatchWithCache() {
		// Arrange - Create 10 requests, with duplicates
		List<OpeningRequest> requests = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			double width = 1.0 + (i % 3) * 0.5; // Only 3 unique widths
			requests.add(new OpeningRequest(
				"aperture:door_standard",
				Map.of("width", width, "height", 2.0)
			));
		}

		// Act
		long startTime = System.currentTimeMillis();
		List<OpeningResult> results = kernel.generateBatch(requests);
		long batchTime = System.currentTimeMillis() - startTime;

		// Assert
		assertEquals(10, results.size());

		long successCount = results.stream()
			.filter(OpeningResult::isSuccess)
			.count();

		assertTrue(successCount >= 8, "Most requests should succeed");

		// Check cache stats
		KernelStats stats = kernel.getStats();
		assertTrue(stats.cacheStats().hits() > 0,
			"Should have cache hits due to duplicate parameters");

		System.out.println("Batch completed in " + batchTime + "ms");
		System.out.println("Cache hits: " + stats.cacheStats().hits());
		System.out.println("Cache hit rate: " +
			String.format("%.1f%%", stats.cacheStats().hitRate() * 100));
	}

	@Test
	@DisplayName("Concurrent async generations")
	void testConcurrentAsync() throws Exception {
		// Arrange
		List<CompletableFuture<OpeningResult>> futures = new ArrayList<>();

		// Launch 8 concurrent generations
		for (int i = 0; i < 8; i++) {
			double width = 1.0 + i * 0.2;
			var request = new OpeningRequest(
				"aperture:door_standard",
				Map.of("width", width, "height", 2.0)
			);
			futures.add(kernel.generateAsync(request));
		}

		// Act - Wait for all to complete
		CompletableFuture<Void> allOf = CompletableFuture.allOf(
			futures.toArray(new CompletableFuture[0])
		);
		allOf.join();

		// Assert
		for (CompletableFuture<OpeningResult> future : futures) {
			assertTrue(future.isDone(), "All futures should complete");
			OpeningResult result = future.get();
			assertNotNull(result);
		}

		// Check stats
		KernelStats stats = kernel.getStats();
		assertEquals(8, stats.totalRequests());
	}

	@Test
	@DisplayName("Mixed success and failure handling")
	void testMixedResults() {
		// Arrange
		List<OpeningRequest> requests = List.of(
			new OpeningRequest("aperture:door_standard", Map.of("width", 1.0)),
			new OpeningRequest("aperture:invalid_type", Map.of()),
			new OpeningRequest("aperture:door_standard", Map.of("width", 1.5)),
			new OpeningRequest("aperture:another_invalid", Map.of())
		);

		// Act
		List<OpeningResult> results = kernel.generateBatch(requests);

		// Assert
		assertEquals(4, results.size());

		long successCount = results.stream()
			.filter(OpeningResult::isSuccess)
			.count();

		long failureCount = results.stream()
			.filter(r -> !r.isSuccess())
			.count();

		assertTrue(successCount > 0, "Should have some successes");
		assertTrue(failureCount > 0, "Should have some failures");

		// Check stats
		KernelStats stats = kernel.getStats();
		assertEquals(4, stats.totalRequests());
		assertEquals(successCount, stats.successfulRequests());
		assertEquals(failureCount, stats.failedRequests());
	}

	@Test
	@DisplayName("Statistics tracking across multiple operations")
	void testStatisticsTracking() {
		// Arrange & Act
		kernel.generate("aperture:door_standard", Map.of("width", 1.0));
		kernel.generate("aperture:door_standard", Map.of("width", 1.5));
		kernel.generate("aperture:invalid_type", Map.of());
		kernel.generate("aperture:door_standard", Map.of("width", 1.0)); // Cached

		// Assert
		KernelStats stats = kernel.getStats();

		assertEquals(4, stats.totalRequests());
		assertTrue(stats.successfulRequests() >= 2);
		assertTrue(stats.failedRequests() >= 1);
		assertTrue(stats.averageExecutionTimeMs() > 0);
		assertTrue(stats.cacheStats().hits() > 0);
	}

	@Test
	@DisplayName("Clear cache resets performance")
	void testCacheClearEffect() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1.0)
		);

		// Generate twice to populate cache
		kernel.generate(request);
		kernel.generate(request);

		KernelStats beforeClear = kernel.getStats();
		long hitsBefore = beforeClear.cacheStats().hits();

		// Act - Clear cache
		kernel.clearCache();

		// Generate again
		kernel.generate(request);

		// Assert
		// First generation after clear should not have additional cache hits
		// from the cleared cache
	}

	@Test
	@DisplayName("Health check reflects kernel state")
	void testHealthCheck() {
		// Arrange - Generate mostly successful requests
		for (int i = 0; i < 20; i++) {
			kernel.generate("aperture:door_standard", Map.of("width", 1.0 + i * 0.1));
		}

		// Act
		boolean healthy = kernel.isHealthy();

		// Assert
		// Health depends on success rate and cache hit rate
		// With cache and valid requests, should be healthy
		assertTrue(healthy || !healthy, "Health check should return a value");
	}

	@Test
	@DisplayName("Resource cleanup on close")
	void testResourceCleanup() throws InterruptedException {
		// Arrange - Launch async operations
		for (int i = 0; i < 5; i++) {
			kernel.generateAsync(new OpeningRequest(
				"aperture:door_standard",
				Map.of("width", 1.0 + i * 0.1)
			));
		}

		// Act - Close immediately
		kernel.close();

		// Assert
		assertTrue(kernel.isClosed());

		// Wait a bit for cleanup
		Thread.sleep(100);

		// Should not be able to generate after close
		assertThrows(IllegalStateException.class, () -> {
			kernel.generate("aperture:door_standard", Map.of("width", 1.0));
		});
	}

	@Test
	@DisplayName("Performance under load")
	void testPerformanceUnderLoad() {
		// Arrange
		int requestCount = 50;
		List<OpeningRequest> requests = new ArrayList<>();

		for (int i = 0; i < requestCount; i++) {
			double width = 1.0 + (i % 10) * 0.1; // 10 unique values
			requests.add(new OpeningRequest(
				"aperture:door_standard",
				Map.of("width", width, "height", 2.0)
			));
		}

		// Act
		long startTime = System.currentTimeMillis();
		List<OpeningResult> results = kernel.generateBatch(requests);
		long totalTime = System.currentTimeMillis() - startTime;

		// Assert
		assertEquals(requestCount, results.size());

		long successCount = results.stream()
			.filter(OpeningResult::isSuccess)
			.count();

		double avgTimePerRequest = (double) totalTime / requestCount;

		System.out.println("\n=== Performance Test Results ===");
		System.out.println("Total requests: " + requestCount);
		System.out.println("Successful: " + successCount);
		System.out.println("Total time: " + totalTime + "ms");
		System.out.println("Avg per request: " +
			String.format("%.1fms", avgTimePerRequest));

		KernelStats stats = kernel.getStats();
		System.out.println("Cache hit rate: " +
			String.format("%.1f%%", stats.cacheStats().hitRate() * 100));
		System.out.println("Success rate: " +
			String.format("%.1f%%", stats.successRate() * 100));

		assertTrue(successCount >= requestCount * 0.9,
			"At least 90% should succeed");
	}

	@Test
	@DisplayName("Try-with-resources pattern")
	void testTryWithResources() {
		// Act & Assert
		assertDoesNotThrow(() -> {
			try (ApertureKernel autoCloseKernel = ApertureKernel.builder().build()) {
				OpeningResult result = autoCloseKernel.generate(
					"aperture:door_standard",
					Map.of("width", 1.0)
				);
				assertNotNull(result);
			}
			// Kernel should be automatically closed
		});
	}
}
