package dev.aperture.opening.geometry.pipeline.decoration;

import dev.aperture.core.component.DecorationComponent;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Reserved for decorative muntins, grilles, and applied ornament.
 */
public final class DecorationGenerator implements ComponentPipelineStep {
	private final DecorationComponent component;

	public DecorationGenerator(DecorationComponent component) {
		this.component = component;
	}

	@Override
	public DecorationComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		// Decoration geometry will be generated from DecorationComponent definitions in a later phase.
	}
}
