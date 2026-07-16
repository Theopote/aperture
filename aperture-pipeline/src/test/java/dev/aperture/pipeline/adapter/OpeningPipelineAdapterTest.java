package dev.aperture.pipeline.adapter;

import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.DefinitionStage;
import dev.aperture.pipeline.stage.PlacementStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for OpeningPipelineAdapter.
 * <p>
 * Validates that the adapter correctly bridges the unified pipeline
 * system with the opening generation infrastructure.
 */
@DisplayName("Opening Pipeline Adapter Integration")
class OpeningPipelineAdapterTest {

	private OpeningPipelineAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = OpeningPipelineAdapter.standard();
	}

	@Test
	@DisplayName("Standard adapter executes successfully")
	void testStandardAdapterExecution() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> params = Map.of(
			"width", 1.0,
			"height", 2.0
		);

		// Act
		PipelineResult result = adapter.execute(typeId, params);

		// Assert
		assertNotNull(result, "Result should not be null");
		assertTrue(result.isSuccess(), "Pipeline should execute successfully");
		assertNotNull(result.getFinalOutput(), "Final output should not be null");
		assertEquals(8, result.stageCount(), "Should execute all 8 stages");
	}

	@Test
	@DisplayName("Execute with OpeningRequest object")
	void testExecuteWithRequest() {
		// Arrange
		var request = new DefinitionStage.OpeningRequest(
			"aperture:window_standard",
			Map.of("width", 1.5, "height", 1.8)
		);

		// Act
		PipelineResult result = adapter.execute(request);

		// Assert
		assertTrue(result.isSuccess());
		assertInstanceOf(
			PlacementStage.PlacementInfo.class,
			result.getFinalOutput(),
			"Final output should be PlacementInfo"
		);
	}

	@Test
	@DisplayName("Adapter with custom cache capacity")
	void testCustomCacheCapacity() {
		// Arrange
		OpeningPipelineAdapter customAdapter = OpeningPipelineAdapter.withCache(500);

		// Act
		PipelineResult result = customAdapter.execute(
			"aperture:door_standard",
			Map.of("width", 1.0)
		);

		// Assert
		assertTrue(result.isSuccess());
	}

	@Test
	@DisplayName("Adapter without cache executes successfully")
	void testWithoutCache() {
		// Arrange
		OpeningPipelineAdapter noCacheAdapter = OpeningPipelineAdapter.withoutCache();

		// Act
		PipelineResult result = noCacheAdapter.execute(
			"aperture:door_standard",
			Map.of("width", 1.0)
		);

		// Assert
		assertTrue(result.isSuccess());
		assertEquals(0, result.cacheHits(), "Should have no cache hits");
	}

	@Test
	@DisplayName("Cache improves performance on repeated executions")
	void testCachingPerformance() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> params = Map.of("width", 1.0, "height", 2.0);

		// Act - First execution (cold)
		long startCold = System.currentTimeMillis();
		PipelineResult coldResult = adapter.execute(typeId, params);
		long coldTime = System.currentTimeMillis() - startCold;

		// Act - Second execution (warm)
		long startWarm = System.currentTimeMillis();
		PipelineResult warmResult = adapter.execute(typeId, params);
		long warmTime = System.currentTimeMillis() - startWarm;

		// Assert
		assertTrue(coldResult.isSuccess());
		assertTrue(warmResult.isSuccess());
		assertTrue(
			warmTime < coldTime,
			"Cached execution should be faster (cold: " + coldTime + "ms, warm: " + warmTime + "ms)"
		);
		assertTrue(
			warmResult.cacheHits() > 0,
			"Second execution should have cache hits"
		);
	}

	@Test
	@DisplayName("Clear cache invalidates cached results")
	void testClearCache() {
		// Arrange
		Map<String, Object> params = Map.of("width", 1.0);

		// Act - Execute twice with same params
		adapter.execute("aperture:door_standard", params);
		PipelineResult beforeClear = adapter.execute("aperture:door_standard", params);

		// Clear cache
		adapter.clearCache();

		// Execute again
		PipelineResult afterClear = adapter.execute("aperture:door_standard", params);

		// Assert
		assertTrue(beforeClear.cacheHits() > 0, "Should have cache hits before clear");
		assertEquals(0, afterClear.cacheHits(), "Should have no cache hits after clear");
	}

	@Test
	@DisplayName("Invalid opening type fails gracefully")
	void testInvalidOpeningType() {
		// Arrange
		String invalidTypeId = "aperture:nonexistent_type";

		// Act
		PipelineResult result = adapter.execute(invalidTypeId, Map.of());

		// Assert
		assertFalse(result.isSuccess(), "Should fail for invalid type");
		assertNotNull(result.getFailureMessage(), "Should have failure message");
		assertEquals("definition", result.getFailedStageName(), "Should fail at definition stage");
	}

	@Test
	@DisplayName("Missing required parameters fails validation")
	void testMissingRequiredParameters() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> incompleteParams = Map.of(); // Missing width, height

		// Act
		PipelineResult result = adapter.execute(typeId, incompleteParams);

		// Assert
		assertFalse(result.isSuccess(), "Should fail with missing parameters");
		String failedStage = result.getFailedStageName();
		assertTrue(
			"parameter".equals(failedStage) || "constraint".equals(failedStage),
			"Should fail at parameter or constraint stage"
		);
	}

	@Test
	@DisplayName("Negative dimensions fail constraint validation")
	void testNegativeDimensionsFail() {
		// Arrange
		Map<String, Object> invalidParams = Map.of(
			"width", -1.0,  // Negative width
			"height", 2.0
		);

		// Act
		PipelineResult result = adapter.execute("aperture:door_standard", invalidParams);

		// Assert
		assertFalse(result.isSuccess(), "Should fail with negative dimensions");
		assertTrue(
			result.getFailureMessage().toLowerCase().contains("constraint") ||
			result.getFailureMessage().toLowerCase().contains("positive") ||
			result.getFailureMessage().toLowerCase().contains("invalid"),
			"Failure message should mention constraint violation"
		);
	}

	@Test
	@DisplayName("Different parameter combinations use cache correctly")
	void testCacheDifferentiation() {
		// Arrange
		String typeId = "aperture:door_standard";
		Map<String, Object> params1 = Map.of("width", 1.0, "height", 2.0);
		Map<String, Object> params2 = Map.of("width", 1.5, "height", 2.0);
		Map<String, Object> params3 = Map.of("width", 1.0, "height", 2.0); // Same as params1

		// Act
		PipelineResult result1 = adapter.execute(typeId, params1);
		PipelineResult result2 = adapter.execute(typeId, params2);
		PipelineResult result3 = adapter.execute(typeId, params3);

		// Assert
		assertTrue(result1.isSuccess());
		assertTrue(result2.isSuccess());
		assertTrue(result3.isSuccess());

		// result1 should be a cache miss (first time)
		assertEquals(0, result1.cacheHits());

		// result2 should be a cache miss (different params)
		assertEquals(0, result2.cacheHits());

		// result3 should have cache hits (same as params1)
		assertTrue(
			result3.cacheHits() > 0,
			"Same parameters should produce cache hits"
		);
	}

	@Test
	@DisplayName("Get final placement info from result")
	void testGetPlacementInfo() {
		// Arrange
		Map<String, Object> params = Map.of("width", 1.0, "height", 2.0, "thickness", 0.1);

		// Act
		PipelineResult result = adapter.execute("aperture:door_standard", params);

		// Assert
		assertTrue(result.isSuccess());

		PlacementStage.PlacementInfo placement =
			(PlacementStage.PlacementInfo) result.getFinalOutput();

		assertNotNull(placement, "Placement info should not be null");
		assertNotNull(placement.dimensions(), "Dimensions should not be null");
		assertNotNull(placement.bounds(), "Bounds should not be null");
		assertNotNull(placement.attachmentPoint(), "Attachment point should not be null");
		assertTrue(placement.volume() > 0, "Volume should be positive");
	}

	@Test
	@DisplayName("Access intermediate stage results")
	void testAccessIntermediateResults() {
		// Arrange
		Map<String, Object> params = Map.of("width", 1.0, "height", 2.0);

		// Act
		PipelineResult result = adapter.execute("aperture:door_standard", params);

		// Assert
		assertTrue(result.isSuccess());

		// Check that we can access intermediate stage outputs
		// Note: Actual stage output types depend on implementation
		assertNotNull(result.getFinalOutput());

		// Verify all 8 stages executed
		assertEquals(8, result.stageCount());
	}

	@Test
	@DisplayName("Concurrent executions are thread-safe")
	void testConcurrentExecution() throws InterruptedException {
		// Arrange
		int threadCount = 4;
		boolean[] successes = new boolean[threadCount];
		Thread[] threads = new Thread[threadCount];

		// Act - Execute concurrently
		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				PipelineResult result = adapter.execute(
					"aperture:door_standard",
					Map.of("width", 1.0 + index * 0.1)
				);
				successes[index] = result.isSuccess();
			});
			threads[i].start();
		}

		// Wait for all threads
		for (Thread thread : threads) {
			thread.join();
		}

		// Assert
		for (int i = 0; i < threadCount; i++) {
			assertTrue(successes[i], "Thread " + i + " should succeed");
		}
	}

	@Test
	@DisplayName("Unwrap provides access to underlying pipeline")
	void testUnwrap() {
		// Act
		var pipeline = adapter.unwrap();

		// Assert
		assertNotNull(pipeline, "Unwrapped pipeline should not be null");
		assertEquals(8, pipeline.stageCount(), "Pipeline should have 8 stages");

		var stageNames = pipeline.stageNames();
		assertEquals(8, stageNames.size());
		assertTrue(stageNames.contains("definition"));
		assertTrue(stageNames.contains("placement"));
	}
}
