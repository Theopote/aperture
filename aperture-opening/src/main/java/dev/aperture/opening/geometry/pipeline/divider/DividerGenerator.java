package dev.aperture.opening.geometry.pipeline.divider;

import dev.aperture.core.component.DividerComponent;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Placeholder for non-mullion divider components (grilles, glazing bars, etc.).
 */
public final class DividerGenerator implements ComponentPipelineStep {
	private final DividerComponent component;

	public DividerGenerator(DividerComponent component) {
		this.component = component;
	}

	@Override
	public DividerComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		// Reserved for future decorative divider geometry.
	}
}
