package dev.aperture.kernel.internal;

import dev.aperture.kernel.GenerationMetrics;
import dev.aperture.kernel.KernelDiagnostic;
import dev.aperture.kernel.KernelErrorCode;
import dev.aperture.kernel.OpeningRequest;
import dev.aperture.kernel.OpeningResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.math.BoundingBox;
import dev.aperture.pipeline.stage.GeometryStage;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.BasicPlacementMetadataStage;

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
		BasicPlacementMetadataStage.PlacementInfo placement = requireStage(
			pipelineResult, "placement", BasicPlacementMetadataStage.PlacementInfo.class
		);
		GeometryStage.GeometryCompilation compilation = requireStage(
			pipelineResult, "geometry", GeometryStage.GeometryCompilation.class
		);
		var geometry = compilation.geometry();
		MeshAssembly meshes = requireStage(pipelineResult, "mesh", MeshAssembly.class);
		BoundingBox collision = requireStage(pipelineResult, "collision", BoundingBox.class);
		dev.aperture.geometry.pipeline.PipelineResult output =
			new dev.aperture.geometry.pipeline.PipelineResult(
				geometry.result(), meshes, geometry.recipe(), collision, collision
			);

		return new OpeningResult.Success(
			request.typeId(), output, placement, buildMetrics(pipelineResult)
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
		dev.aperture.pipeline.PipelineDiagnostic source = ((PipelineResult.Failure) pipelineResult).diagnostic();
		KernelDiagnostic diagnostic = new KernelDiagnostic(
			KernelErrorCode.valueOf(source.code().name()), source.severity(), source.stage(),
			source.componentPath(), source.parameterPath(), source.message(), source.cause()
		);
		return new OpeningResult.Failure(request.typeId(), diagnostic);
	}

	public static GenerationMetrics buildMetrics(PipelineResult pipelineResult) {
		java.time.Duration totalTime = pipelineResult.executionTime();
		int cacheHits = pipelineResult.cacheHits();
		int cacheMisses = pipelineResult.stageCount() - cacheHits;
		Map<String, java.time.Duration> stageTimings = new HashMap<>();
		var metrics = pipelineResult.getMetrics();

		if (metrics != null) {
			String[] stageNames = {
				"definition", "parameter", "constraint", "component",
				"geometry", "mesh", "collision", "placement"
			};
			for (String stage : stageNames) {
				java.time.Duration time = metrics.stageTime(stage);
				if (!time.isZero()) {
					stageTimings.put(stage, time);
				}
			}
		}

		return new GenerationMetrics(totalTime, cacheHits, cacheMisses, stageTimings);
	}
}