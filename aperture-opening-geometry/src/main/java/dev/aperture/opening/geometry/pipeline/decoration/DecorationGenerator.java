package dev.aperture.opening.geometry.pipeline.decoration;

import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;

/**
 * Reserved for decorative muntins, grilles, and applied ornament.
 */
public final class DecorationGenerator implements PipelineStep {
	public static final String STEP_ID = "decoration";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		// Decoration geometry will be generated from DecorationComponent definitions in a later phase.
	}
}
