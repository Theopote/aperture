package dev.aperture.kernel.internal;

import dev.aperture.kernel.GenerationMetrics;
import dev.aperture.kernel.OpeningRequest;
import dev.aperture.kernel.OpeningResult;
import dev.aperture.math.BoundingBox;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.PlacementStage;

import java.util.HashMap;
import java.util.Map;

/** Maps unified pipeline results to the public Kernel result contract. */
public final class ResultMapper {
	private ResultMapper() {
	}

	public static OpeningResult map(OpeningRequest request, PipelineResult pipelineResult) {
		return pipelineResult.isSuccess()
			? mapSuccess(request, (PipelineResult.Success) pipelineResult)
			: mapFailure(request, pipelineResult);
	}

	private static OpeningResult.Success mapSuccess(
		OpeningRequest request,
		PipelineResult.Success pipelineResult
	) {
		PlacementStage.PlacementInfo placement = requireStage(
			pipelineResult, "placement", PlacementStage.PlacementInfo.class
		);
		dev.aperture.geometry.pipeline.PipelineResult output = requireStage(
			pipelineResult, "geometry", dev.aperture.geometry.pipeline.PipelineResult.class
		);
		BoundingBox collision = requireStage(pipelineResult, "collision", BoundingBox.class);

		return new OpeningResult.Success(
			request.typeId(),
			output.withCollisionAndFootprint(collision, collision),
			placement,
			buildMetrics(pipelineResult)
		);
	}

	private static <T> T requireStage(
		PipelineResult.Success result,
		String stageName,
		Class<T> expectedType
	) {
		Object value = result.getStageValue(stageName).orElseThrow(
			() -> new IllegalStateException("Missing pipeline stage output: " + stageName)
		);
		if (!expectedType.isInstance(value)) {
			throw new IllegalStateException(
				"Expected " + expectedType.getSimpleName() + " from " + stageName
					+ " stage but got " + value.getClass().getName()
			);
		}
		return expectedType.cast(value);
	}

	private static OpeningResult.Failure mapFailure(
		OpeningRequest request,
		PipelineResult pipelineResult
	) {
		String failedStage = pipelineResult.getFailedStageName();
		String errorMessage = pipelineResult.getFailureMessage();
		return new OpeningResult.Failure(
			request.typeId(),
			failedStage == null ? "unknown" : failedStage,
			errorMessage == null ? "Unknown error" : errorMessage,
			pipelineResult.getFailureCause()
		);
	}

	public static GenerationMetrics buildMetrics(PipelineResult pipelineResult) {
		long totalTime = pipelineResult.executionTimeMs();
		int cacheHits = pipelineResult.cacheHits();
		int cacheMisses = pipelineResult.stageCount() - cacheHits;
		Map<String, Long> stageTimings = new HashMap<>();
		var metrics = pipelineResult.getMetrics();

		if (metrics != null) {
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

		return new GenerationMetrics(totalTime, cacheHits, cacheMisses, stageTimings);
	}
}