package dev.aperture.geometry.pipeline.accessory;

import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.pipeline.OpeningParameters;
import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;

/**
 * Generates accessories such as mullions and future hardware mounts.
 */
public final class AccessoryGenerator implements PipelineStep {
	public static final String STEP_ID = "accessory";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningLayout layout = context.layout();
		OpeningParameters parameters = context.openingParameters();
		ProfileCurve profile = context.resolvedProfiles().frame().curve();

		for (int i = 1; i <= parameters.mullions(); i++) {
			double t = (double) i / (parameters.mullions() + 1);
			double x = layout.frameFace() + layout.innerWidth() * t - layout.frameFace() / 2.0;
			assembly.addSolid(FrameRailBuilder.miteredRail(
				"frame.mullion." + i,
				profile,
				new Vec3d(x, layout.frameFace(), 0),
				new Vec3d(x, layout.height() - layout.frameFace(), 0),
				FrameRailBuilder.axisX(),
				FrameRailBuilder.axisZ()
			));
		}
	}
}
