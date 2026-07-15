package dev.aperture.geometry.pipeline.trim;

import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;

/**
 * Reserved for interior/exterior trim and casing geometry.
 */
public final class TrimGenerator implements PipelineStep {
	public static final String STEP_ID = "trim";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		// Trim geometry will be generated from TrimComponent definitions in a later phase.
	}
}
