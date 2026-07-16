package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.opening.component.ComponentPlanBuilder;

import java.util.List;

/**
 * @deprecated Use {@link dev.aperture.opening.component.ComponentPlanBuilder}.
 */
@Deprecated
public final class ComponentPipelinePlanner {
	private ComponentPipelinePlanner() {
	}

	public static OpeningPipeline pipelineFor(ComponentAssembly assembly) {
		return OpeningPipeline.forAssembly(assembly);
	}

	public static List<String> plannedStepIds(ComponentAssembly assembly) {
		return ComponentPlanBuilder.plannedStepIds(assembly);
	}
}
