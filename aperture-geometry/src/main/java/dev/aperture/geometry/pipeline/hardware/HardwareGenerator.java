package dev.aperture.geometry.pipeline.hardware;

import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;

/**
 * Reserved for hinges, handles, and locking hardware geometry.
 */
public final class HardwareGenerator implements PipelineStep {
	public static final String STEP_ID = "hardware";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		// Hardware mounts will be generated from HardwareComponent definitions in a later phase.
	}
}
