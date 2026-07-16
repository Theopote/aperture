package dev.aperture.kernel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApertureKernel basic functionality.
 */
@DisplayName("Aperture Kernel")
class ApertureKernelTest {

	private ApertureKernel kernel;

	@BeforeEach
	void setUp() {
		kernel = ApertureKernel.builder()
			.withCacheCapacity(10)
			.build();
	}

	@AfterEach
	void tearDown() {
		if (kernel != null && !kernel.isClosed()) {
			kernel.close();
		}
	}

	@Test
	@DisplayName("Generate opening successfully")
	void testGenerateSuccess() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> params = Map.of(
			"width", 1000.0,
			"height", 2000.0
		);

		// Act
		OpeningResult result = kernel.generate(typeId, params);

		// Assert
		assertNotNull(result, "Result should not be null");
		assertTrue(result.isSuccess(), "Generation should succeed: " + result);
		assertEquals(typeId, result.typeId());
	}

	@Test
	@DisplayName("Generate with OpeningRequest")
	void testGenerateWithRequest() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1000.0)
		);

		// Act
		OpeningResult result = kernel.generate(request);

		// Assert
		assertNotNull(result);
		assertTrue(result.isSuccess());
	}

	@Test
	@DisplayName("Generate with custom options")
	void testGenerateWithOptions() {
		// Arrange
		var options = OpeningOptions.DEFAULT
			.withCache(false)
			.withMeshQuality(OpeningOptions.MeshQuality.LOW);

		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1000.0),
			options
		);

		// Act
		OpeningResult result = kernel.generate(request);

		// Assert
		assertNotNull(result);
	}

	@Test
	@DisplayName("Failed generation returns Failure result")
	void testGenerateFailure() {
		// Arrange - Invalid type ID
		String invalidTypeId = "aperture:nonexistent_type";

		// Act
		OpeningResult result = kernel.generate(invalidTypeId, Map.of());

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess(), "Generation should fail");

		var failure = result.asFailure();
		assertEquals(invalidTypeId, failure.typeId());
		assertNotNull(failure.errorMessage());
	}

	@Test
	@DisplayName("Generate with missing parameters fails")
	void testGenerateMissingParameters() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> emptyParams = Map.of();

		// Act
		OpeningResult result = kernel.generate(typeId, emptyParams);

		// Assert
		assertFalse(result.isSuccess(), "Should fail with missing parameters");
	}

	@Test
	@DisplayName("Successful result contains placement info")
	void testSuccessResultPlacement() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1000.0, "height", 2000.0)
		);

		// Act
		OpeningResult result = kernel.generate(request);

		// Assert
		if (result.isSuccess()) {
			var success = result.asSuccess();
			assertNotNull(success.placement(), "Placement should not be null");
			assertNotNull(success.metrics(), "Metrics should not be null");
		}
	}

	@Test
	@DisplayName("Metrics contain timing information")
	void testMetrics() {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1000.0, "height", 2000.0)
		);

		// Act
		OpeningResult result = kernel.generate(request);

		// Assert
		if (result.isSuccess()) {
			var success = result.asSuccess();
			GenerationMetrics metrics = success.metrics();

			assertTrue(metrics.totalTimeMs() >= 0, "Total time should be non-negative");
			assertNotNull(metrics.stageTimings(), "Stage timings should not be null");
		}
	}

	@Test
	@DisplayName("Null request throws exception")
	void testNullRequest() {
		assertThrows(NullPointerException.class, () -> {
			kernel.generate((OpeningRequest) null);
		});
	}

	@Test
	@DisplayName("Null type ID throws exception")
	void testNullTypeId() {
		assertThrows(NullPointerException.class, () -> {
			kernel.generate(null, Map.of());
		});
	}

	@Test
	@DisplayName("Null parameters throws exception")
	void testNullParameters() {
		assertThrows(NullPointerException.class, () -> {
			kernel.generate("aperture:door_standard", null);
		});
	}

	@Test
	@DisplayName("Invalid type ID format throws exception")
	void testInvalidTypeIdFormat() {
		// Arrange
		var request = new OpeningRequest("invalid_format", Map.of());

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			request.validate();
		});
	}

	@Test
	@DisplayName("Batch generation")
	void testBatchGeneration() {
		// Arrange
		List<OpeningRequest> requests = List.of(
			new OpeningRequest("aperture:door_standard", Map.of("width", 1000.0)),
			new OpeningRequest("aperture:door_standard", Map.of("width", 1.5)),
			new OpeningRequest("aperture:door_standard", Map.of("width", 2.0))
		);

		// Act
		List<OpeningResult> results = kernel.generateBatch(requests);

		// Assert
		assertNotNull(results);
		assertEquals(3, results.size(), "Should return result for each request");
	}

	@Test
	@DisplayName("Empty batch returns empty list")
	void testEmptyBatch() {
		// Act
		List<OpeningResult> results = kernel.generateBatch(List.of());

		// Assert
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	@DisplayName("Async generation completes")
	void testAsyncGeneration() throws Exception {
		// Arrange
		var request = new OpeningRequest(
			"aperture:door_standard",
			Map.of("width", 1000.0)
		);

		// Act
		CompletableFuture<OpeningResult> future = kernel.generateAsync(request);

		// Assert
		assertNotNull(future);
		OpeningResult result = future.get(10, TimeUnit.SECONDS);
		assertNotNull(result);
	}

	@Test
	@DisplayName("Multiple async generations")
	void testMultipleAsyncGenerations() throws Exception {
		// Arrange
		var request1 = new OpeningRequest("aperture:door_standard", Map.of("width", 1000.0));
		var request2 = new OpeningRequest("aperture:door_standard", Map.of("width", 1.5));

		// Act
		CompletableFuture<OpeningResult> future1 = kernel.generateAsync(request1);
		CompletableFuture<OpeningResult> future2 = kernel.generateAsync(request2);

		// Wait for both
		CompletableFuture.allOf(future1, future2).get(10, TimeUnit.SECONDS);

		// Assert
		assertTrue(future1.isDone());
		assertTrue(future2.isDone());
		assertNotNull(future1.get());
		assertNotNull(future2.get());
	}

	@Test
	@DisplayName("Get kernel statistics")
	void testGetStats() {
		// Arrange
		kernel.generate("aperture:door_standard", Map.of("width", 1000.0));
		kernel.generate("aperture:door_standard", Map.of("width", 1.5));

		// Act
		KernelStats stats = kernel.getStats();

		// Assert
		assertNotNull(stats);
		assertEquals(2, stats.totalRequests(), "Should track request count");
		assertTrue(stats.successfulRequests() <= stats.totalRequests());
	}

	@Test
	@DisplayName("Reset statistics")
	void testResetStats() {
		// Arrange
		kernel.generate("aperture:door_standard", Map.of("width", 1000.0));
		assertEquals(1, kernel.getStats().totalRequests());

		// Act
		kernel.resetStats();

		// Assert
		KernelStats stats = kernel.getStats();
		assertEquals(0, stats.totalRequests(), "Stats should be reset");
	}

	@Test
	@DisplayName("Clear cache")
	void testClearCache() {
		// Arrange
		var request = new OpeningRequest("aperture:door_standard", Map.of("width", 1000.0));
		kernel.generate(request); // First call
		kernel.generate(request); // Second call (cached)

		// Act
		kernel.clearCache();

		// No exception thrown
		assertDoesNotThrow(() -> kernel.clearCache());
	}

	@Test
	@DisplayName("Check if kernel is closed")
	void testIsClosed() {
		// Before close
		assertFalse(kernel.isClosed(), "Kernel should not be closed initially");

		// After close
		kernel.close();
		assertTrue(kernel.isClosed(), "Kernel should be closed after close()");
	}

	@Test
	@DisplayName("Operations after close throw exception")
	void testOperationsAfterClose() {
		// Arrange
		kernel.close();

		// Act & Assert
		assertThrows(IllegalStateException.class, () -> {
			kernel.generate("aperture:door_standard", Map.of());
		});
	}

	@Test
	@DisplayName("Close can be called multiple times")
	void testMultipleClose() {
		// Act & Assert
		assertDoesNotThrow(() -> {
			kernel.close();
			kernel.close(); // Second close should be safe
		});
	}

	@Test
	@DisplayName("List registered types")
	void testListTypes() {
		// Act
		var types = kernel.listTypes();

		// Assert
		assertNotNull(types);
		// May be empty if no types registered
	}

	@Test
	@DisplayName("Get definition for type")
	void testGetDefinition() {
		// Act
		var definition = kernel.getDefinition("aperture:door_standard");

		// Assert
		assertNotNull(definition);
		// May be empty if type not registered
	}
}
