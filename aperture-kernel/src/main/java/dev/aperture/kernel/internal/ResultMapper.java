package dev.aperture.kernel.internal;

import dev.aperture.kernel.GenerationMetrics;
import dev.aperture.kernel.OpeningRequest;
import dev.aperture.kernel.OpeningResult;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.PlacementStage;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Pipeline results to Kernel results.
 * <p>
 * Package-private, not part of public API.
 */
public final class ResultMapper {

	private ResultMapper() {
		// Utility class
	}

	/**
	 * Map PipelineResult to OpeningResult.
	 */
	public static OpeningResult map(OpeningRequest request, PipelineResult pipelineResult) {
		if (pipelineResult.isSuccess()) {
			return mapSuccess(request, pipelineResult);
		} else {
			return mapFailure(request, pipelineResult);
		}
	}

	private static OpeningResult.Success mapSuccess(
		OpeningRequest request,
		PipelineResult pipelineResult
	) {
		// Extract placement info from final output
		Object finalOutput = pipelineResult.getFinalOutput();

		if (!(finalOutput instanceof PlacementStage.PlacementInfo placement)) {
			throw new IllegalStateException(
				"Expected PlacementInfo but got: " +
				(finalOutput == null ? "null" : finalOutput.getClass().getName())
			);
		}

		// Build metrics
		GenerationMetrics metrics = buildMetrics(pipelineResult);

		return new OpeningResult.Success(
			request.typeId(),
			placement,
			metrics
		);
	}

	private static OpeningResult.Failure mapFailure(
		OpeningRequest request,
		PipelineResult pipelineResult
	) {
		String failedStage = pipelineResult.getFailedStageName();
		String errorMessage = pipelineResult.getFailureMessage();
		Throwable cause = pipelineResult.getFailureCause();

		if (failedStage == null) {
			failedStage = "unknown";
		}
		if (errorMessage == null) {
			errorMessage = "Unknown error";
		}

		return new OpeningResult.Failure(
			request.typeId(),
			failedStage,
			errorMessage,
			cause
		);
	}

	private static GenerationMetrics buildMetrics(PipelineResult pipelineResult) {
		long totalTime = pipelineResult.executionTimeMs();
		int cacheHits = pipelineResult.cacheHits();

		// Calculate cache misses (stages executed - cache hits)
		int stagesExecuted = pipelineResult.stageCount();
		int cacheMisses = stagesExecuted - cacheHits;

		// Extract stage timings
		Map<String, Long> stageTimings = new HashMap<>();
		var metrics = pipelineResult.getMetrics();

		if (metrics != null) {
			// Get all stage names and their timings
			String[] stageNames = {
				"definition", "parameter", "constraint", "component",
				"geometry", "mesh", "collision", "placement"
			};

			for (String stage : stageNames) {
				long time = metrics.getStageTime(stage);
				if (time > 0) {
					stageTimings.put(stage, time);
				}
			}
		}

		return new GenerationMetrics(
			totalTime,
			cacheHits,
			cacheMisses,
			stageTimings
		);
	}
}
