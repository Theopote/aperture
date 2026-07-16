package dev.aperture.opening.geometry.pipeline;

import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;

/**
 * One step in the opening generator pipeline.
 */
public interface PipelineStep {
	String id();

	void execute(OpeningPipelineContext context, GeometryCompilationTarget target);
}
