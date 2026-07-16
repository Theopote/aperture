package dev.aperture.pipeline;

import dev.aperture.pipeline.stage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete 8-stage opening generation pipeline.
 */
@DisplayName("Complete Pipeline Integration")
class PipelineIntegrationTest {

	private Pipeline pipeline;
	private PipelineCache cache;

	@BeforeEach
	void setUp() {
		cache = new PipelineCache(100);

		// Build complete 8-stage pipeline
		pipeline = Pipeline.builder()
			.addStage(new DefinitionStage(PipelineTestFactory.registry()))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage())
			.addStage(PipelineTestFactory.componentStage())
			.addStage(PipelineTestFactory.geometryStage())
			.addStage(PipelineTestFactory.meshStage())
			.addStage(new BoundingBoxCollisionStage())
			.addStage(new BasicPlacementMetadataStage())
			.withCache(cache)
			.build();
	}

	@Test
	@DisplayName("Execute complete pipeline successfully")
	void testCompletePipelineExecution() {
		// Arrange
		String openingTypeId = "aperture:door_standard";
		Map<String, Object> userParams = Map.of(
			"width", 1000.0,
			"height", 2000.0,
			"thickness", 100.0
		);

		var input = new DefinitionStage.OpeningRequest(openingTypeId, userParams);

		// Act
		PipelineResult result = pipeline.execute(input);

		// Assert
		assertTrue(result.isSuccess(), "Pipeline should complete successfully: " + result);
		assertNotNull(result.getFinalOutput(), "Final output should not be null");
		assertEquals(8, result.stageCount(), "Should execute all 8 stages");
		assertTrue(result.executionTime().toNanos() > 0, "Should record execution time");

		// Verify final output is PlacementInfo
		assertInstanceOf(
			BasicPlacementMetadataStage.PlacementInfo.class,
			result.getFinalOutput(),
			"Final output should be PlacementInfo"
		);
	}

	@Test
	@DisplayName("Pipeline caching reduces execution time")
	void testPipelineCaching() {
		// Arrange
		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());

		// Act - First execution (no cache)
		PipelineResult result1 = pipeline.execute(input);
		long firstTime = result1.executionTime().toNanos();

		// Act - Second execution (with cache)
		PipelineResult result2 = pipeline.execute(input);
		long secondTime = result2.executionTime().toNanos();

		// Assert
		assertTrue(result1.isSuccess(), "First execution should succeed");
		assertTrue(result2.isSuccess(), "Second execution should succeed");
		assertTrue(
			secondTime < firstTime,
			"Cached execution should be faster (first: " + firstTime + "ms, second: " + secondTime + "ms)"
		);

		// Verify cache metrics
		assertTrue(
			result2.cacheHits() > 0,
			"Should have cache hits on second execution"
		);
	}

	@Test
	@DisplayName("Pipeline short-circuits on stage failure")
	void testPipelineShortCircuit() {
		// Arrange - Pipeline with failing stage at position 3
		Pipeline failingPipeline = Pipeline.builder()
			.addStage(new DefinitionStage(PipelineTestFactory.registry()))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage()) // This will fail with invalid constraints
			.addStage(PipelineTestFactory.componentStage())
			.addStage(PipelineTestFactory.geometryStage())
			.addStage(PipelineTestFactory.meshStage())
			.addStage(new BoundingBoxCollisionStage())
			.addStage(new BasicPlacementMetadataStage())
			.build();

		// Input with constraint violations
		var input = new DefinitionStage.OpeningRequest(
			"aperture:door_standard",
			Map.of("width", -1.0) // Negative width should fail constraint validation
		);

		// Act
		PipelineResult result = failingPipeline.execute(input);

		// Assert
		assertFalse(result.isSuccess(), "Pipeline should fail");
		assertNotNull(result.getFailureMessage(), "Should have failure message");
		assertTrue(
			result.stageCount() <= 3,
			"Should stop at or before constraint stage (stopped at stage " + result.stageCount() + ")"
		);
		assertNull(result.getFinalOutput(), "Should not have final output");
	}

	@Test
	@DisplayName("Pipeline collects metrics for all stages")
	void testPipelineMetrics() {
		// Arrange
		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());

		// Act
		PipelineResult result = pipeline.execute(input);

		// Assert
		assertTrue(result.isSuccess(), "Pipeline should succeed");

		PipelineMetrics metrics = result.getMetrics();
		assertNotNull(metrics, "Should have metrics");
		assertEquals(8, metrics.stageCount(), "Should have metrics for all 8 stages");

		// Verify each stage has timing data
		String[] expectedStages = {
			"definition", "parameter", "constraint", "component",
			"geometry", "mesh", "collision", "placement"
		};

		for (String stageName : expectedStages) {
			long stageTime = metrics.stageTime(stageName).toNanos();
			assertTrue(
				stageTime >= 0,
				"Stage '" + stageName + "' should have timing data"
			);
		}
	}

	@Test
	@DisplayName("Pipeline handles stage skipping correctly")
	void testStageSkipping() {
		// Arrange - Create pipeline with stage that can be skipped
		var skipStage = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "skip-test";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				return new StageResult.Success<>(input + "-processed");
			}

			@Override
			public boolean canSkip(String input, StageContext ctx) {
				return input.contains("skip");
			}
		};

		Pipeline testPipeline = Pipeline.builder()
			.addStage(skipStage)
			.build();

		// Act - Execute with skippable input
		PipelineResult result = testPipeline.execute("skip-me");

		// Assert
		assertTrue(result.isSuccess(), "Pipeline should succeed");
		// Note: Skipped stages still complete successfully, they just use cached/default values
	}

	@Test
	@DisplayName("Cache respects capacity limit")
	void testCacheCapacity() {
		// Arrange
		PipelineCache smallCache = new PipelineCache(2); // Only 2 entries
		Pipeline cachedPipeline = Pipeline.builder()
			.addStage(new DefinitionStage(PipelineTestFactory.registry()))
			.withCache(smallCache)
			.build();

		// Act - Execute 3 different requests
		cachedPipeline.execute(new DefinitionStage.OpeningRequest("type1", Map.of()));
		cachedPipeline.execute(new DefinitionStage.OpeningRequest("type2", Map.of()));
		cachedPipeline.execute(new DefinitionStage.OpeningRequest("type3", Map.of()));

		// Assert
		assertTrue(
			smallCache.size() <= 2,
			"Cache should not exceed capacity (size: " + smallCache.size() + ")"
		);
	}

	@Test
	@DisplayName("Pipeline preserves type safety across stages")
	void testTypeSafety() {
		// This test validates that the generic type constraints prevent type errors
		// The fact that this compiles demonstrates type safety at compile time

		Pipeline typedPipeline = Pipeline.builder()
			// Stage 1: OpeningRequest -> ResolvedDefinition
			.addStage(new DefinitionStage(PipelineTestFactory.registry()))
			// Stage 2: ResolvedDefinition -> ParameterSet
			.addStage(new ParameterStage())
			// If we tried to add a stage with incompatible types, this would not compile
			.build();

		var input = new DefinitionStage.OpeningRequest("aperture:door_standard", Map.of());
		PipelineResult result = typedPipeline.execute(input);

		assertTrue(result.isSuccess(), "Type-safe pipeline should execute successfully");
	}
}
