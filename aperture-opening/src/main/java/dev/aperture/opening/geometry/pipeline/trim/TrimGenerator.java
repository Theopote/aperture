package dev.aperture.opening.geometry.pipeline.trim;

import dev.aperture.core.component.TrimComponent;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Reserved for interior/exterior trim and casing geometry.
 */
public final class TrimGenerator implements ComponentPipelineStep {
	private final TrimComponent component;

	public TrimGenerator(TrimComponent component) {
		this.component = component;
	}

	@Override
	public TrimComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		// Trim geometry will be generated from TrimComponent definitions in a later phase.
	}
}
