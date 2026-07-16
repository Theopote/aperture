package dev.aperture.opening.component;

import dev.aperture.opening.geometry.pipeline.PipelineStep;

import java.util.List;

/**
 * Ordered geometry steps — one {@link dev.aperture.opening.geometry.pipeline.ComponentPipelineStep}
 * per component instance in the assembly.
 */
public record ComponentPlan(List<PipelineStep> steps) {
	public ComponentPlan {
		steps = List.copyOf(steps);
	}

	public List<String> stepIds() {
		return steps.stream().map(PipelineStep::id).toList();
	}
}
