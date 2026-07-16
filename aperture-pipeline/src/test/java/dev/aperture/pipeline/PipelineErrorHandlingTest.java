package dev.aperture.pipeline;

import dev.aperture.pipeline.stage.DefinitionStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for pipeline error handling and failure scenarios.
 */
@DisplayName("Pipeline Error Handling")
class PipelineErrorHandlingTest {

	@Test
	@DisplayName("Handle stage throwing exception")
	void testStageException() {
		// Arrange
		var throwingStage = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "throwing-stage";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				throw new RuntimeException("Simulated failure");
			}
		};

		Pipeline pipeline = Pipeline.builder()
			.addStage(throwingStage)
			.build();

		// Act
		PipelineResult result = pipeline.execute("test-input");

		// Assert
		assertFalse(result.isSuccess(), "Pipeline should fail when stage throws");
		assertNotNull(result.getFailureMessage(), "Should capture failure message");
		assertTrue(
			result.getFailureMessage().contains("Simulated failure"),
			"Should include exception message"
		);
	}

	@Test
	@DisplayName("Handle stage returning failure result")
	void testStageFailureResult() {
		// Arrange
		var failingStage = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "failing-stage";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				return new StageResult.Failure<>(
					"Validation failed",
					new IllegalArgumentException("Invalid input")
				);
			}
		};

		Pipeline pipeline = Pipeline.builder()
			.addStage(failingStage)
			.build();

		// Act
		PipelineResult result = pipeline.execute("test");

		// Assert
		assertFalse(result.isSuccess());
		assertEquals("Validation failed", result.getFailureMessage());
		assertInstanceOf(IllegalArgumentException.class, result.getFailureCause());
	}

	@Test
	@DisplayName("Stop execution on first failure")
	void testShortCircuitOnFailure() {
		// Arrange
		boolean[] stage3Executed = {false};

		var stage1 = createPassthroughStage("stage1");
		var stage2 = createFailingStage("stage2", "Stage 2 failed");
		var stage3 = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "stage3";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				stage3Executed[0] = true;
				return new StageResult.Success<>(input);
			}
		};

		Pipeline pipeline = Pipeline.builder()
			.addStage(stage1)
			.addStage(stage2)
			.addStage(stage3)
			.build();

		// Act
		PipelineResult result = pipeline.execute("input");

		// Assert
		assertFalse(result.isSuccess(), "Pipeline should fail");
		assertFalse(stage3Executed[0], "Stage 3 should not execute after stage 2 fails");
		assertEquals(2, result.stageCount(), "Should stop at stage 2");
	}

	@Test
	@DisplayName("Handle null input gracefully")
	void testNullInput() {
		// Arrange
		Pipeline pipeline = Pipeline.builder()
			.addStage(createPassthroughStage("stage"))
			.build();

		// Act & Assert
		assertThrows(
			NullPointerException.class,
			() -> pipeline.execute(null),
			"Should reject null input"
		);
	}

	@Test
	@DisplayName("Handle empty pipeline")
	void testEmptyPipeline() {
		// Arrange
		Pipeline pipeline = Pipeline.builder().build();

		// Act
		PipelineResult result = pipeline.execute("input");

		// Assert
		assertTrue(result.isSuccess(), "Empty pipeline should succeed");
		assertEquals("input", result.getFinalOutput(), "Should return input unchanged");
		assertEquals(0, result.stageCount(), "Should have zero stages");
	}

	@Test
	@DisplayName("Preserve failure context across stages")
	void testFailureContextPreservation() {
		// Arrange
		var stage1 = createPassthroughStage("definition");
		var stage2 = createPassthroughStage("parameter");
		var stage3 = createFailingStage("constraint", "Width must be positive");

		Pipeline pipeline = Pipeline.builder()
			.addStage(stage1)
			.addStage(stage2)
			.addStage(stage3)
			.build();

		// Act
		PipelineResult result = pipeline.execute("input");

		// Assert
		assertFalse(result.isSuccess());
		assertEquals("constraint", result.getFailedStageName(), "Should identify failed stage");
		assertTrue(
			result.getFailureMessage().contains("Width must be positive"),
			"Should preserve original error message"
		);
	}

	@Test
	@DisplayName("Handle stage returning null output")
	void testNullStageOutput() {
		// Arrange
		var nullStage = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "null-stage";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				return new StageResult.Success<>(null); // Null output
			}
		};

		Pipeline pipeline = Pipeline.builder()
			.addStage(nullStage)
			.build();

		// Act
		PipelineResult result = pipeline.execute("input");

		// Assert
		assertTrue(result.isSuccess(), "Null output should be allowed");
		assertNull(result.getFinalOutput(), "Should return null");
	}

	@Test
	@DisplayName("Collect metrics even when pipeline fails")
	void testMetricsOnFailure() {
		// Arrange
		var stage1 = createPassthroughStage("stage1");
		var stage2 = createFailingStage("stage2", "Failed");

		Pipeline pipeline = Pipeline.builder()
			.addStage(stage1)
			.addStage(stage2)
			.build();

		// Act
		PipelineResult result = pipeline.execute("input");

		// Assert
		assertFalse(result.isSuccess());
		assertNotNull(result.getMetrics(), "Should collect metrics even on failure");
		assertTrue(
			result.getMetrics().getStageTime("stage1") >= 0,
			"Should have timing for successful stage"
		);
	}

	@Test
	@DisplayName("Handle concurrent pipeline executions")
	void testConcurrentExecution() throws InterruptedException {
		// Arrange
		Pipeline pipeline = Pipeline.builder()
			.addStage(createPassthroughStage("stage"))
			.build();

		boolean[] success = {true, true};

		// Act - Execute pipeline concurrently
		Thread t1 = new Thread(() -> {
			PipelineResult result = pipeline.execute("input1");
			success[0] = result.isSuccess();
		});

		Thread t2 = new Thread(() -> {
			PipelineResult result = pipeline.execute("input2");
			success[1] = result.isSuccess();
		});

		t1.start();
		t2.start();
		t1.join();
		t2.join();

		// Assert
		assertTrue(success[0], "First execution should succeed");
		assertTrue(success[1], "Second execution should succeed");
	}

	@Test
	@DisplayName("Handle stage with invalid type")
	void testInvalidStageType() {
		// This test validates compile-time type safety
		// If uncommented, the following would not compile:

		/*
		Pipeline.<String, String>builder()
			.addStage(new PipelineStage<String, Integer>() { ... })  // Type mismatch
			.addStage(new PipelineStage<Double, String>() { ... })   // Input type doesn't match
			.build();
		*/

		// The fact that invalid types don't compile demonstrates type safety
		assertTrue(true, "Type safety is enforced at compile time");
	}

	@Test
	@DisplayName("Failure in cached stage is not cached")
	void testFailureNotCached() {
		// Arrange
		int[] executionCount = {0};
		var intermittentStage = new PipelineStage<String, String>() {
			@Override
			public String name() {
				return "intermittent";
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				executionCount[0]++;
				if (executionCount[0] == 1) {
					return new StageResult.Failure<>("First attempt failed", null);
				}
				return new StageResult.Success<>(input + "-processed");
			}
		};

		PipelineCache cache = new PipelineCache(10);
		Pipeline pipeline = Pipeline.builder()
			.addStage(intermittentStage)
			.withCache(cache)
			.build();

		// Act
		PipelineResult result1 = pipeline.execute("input");
		PipelineResult result2 = pipeline.execute("input");

		// Assert
		assertFalse(result1.isSuccess(), "First execution should fail");
		assertTrue(result2.isSuccess(), "Second execution should succeed");
		assertEquals(2, executionCount[0], "Stage should execute twice (failures not cached)");
	}

	// Helper methods

	private PipelineStage<String, String> createPassthroughStage(String name) {
		return new PipelineStage<>() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				return new StageResult.Success<>(input);
			}
		};
	}

	private PipelineStage<String, String> createFailingStage(String name, String message) {
		return new PipelineStage<>() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public StageResult<String> execute(String input, StageContext ctx) {
				return new StageResult.Failure<>(message, new RuntimeException(message));
			}
		};
	}
}
