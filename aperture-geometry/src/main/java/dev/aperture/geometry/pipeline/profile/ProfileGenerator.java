package dev.aperture.geometry.pipeline.profile;

import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.ResolvedProfiles;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.geometry.profile.ProfileDefinition;

import java.util.Optional;

/**
 * Resolves and scales catalog profiles for the current opening instance.
 */
public final class ProfileGenerator implements PipelineStep {
	public static final String STEP_ID = "profile";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		ProfileDefinition frame = context.scaledFrameProfile();
		Optional<ProfileDefinition> panel = context.hasComponent("panel")
			? Optional.of(context.scaledPanelProfile())
			: Optional.empty();
		context.setResolvedProfiles(new ResolvedProfiles(frame, panel));
	}
}
