package dev.aperture.opening.component;

import dev.aperture.opening.geometry.pipeline.PipelineStep;

import java.util.List;

/**
 * Ordered geometry steps selected from a {@link dev.aperture.core.component.ComponentAssembly}.
 * Door, window, and curtain wall differ only by which steps appear here.
 */
public record ComponentPlan(List<PipelineStep> steps) {
	public ComponentPlan {
		steps = List.copyOf(steps);
	}

	public List<String> stepIds() {
		return steps.stream().map(PipelineStep::id).toList();
	}
}
