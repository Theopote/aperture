package dev.aperture.opening.geometry.pipeline.header;

import dev.aperture.core.geometry.Vec3d;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;

/**
 * Generates the structural header / head member above the opening frame.
 */
public final class HeaderGenerator implements PipelineStep {
	public static final String STEP_ID = "header";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningLayout layout = context.layout();
		ProfileCurve profile = context.resolvedProfiles().frame().curve();
		double y = layout.height() - layout.frameFace();
		assembly.addSolid(FrameRailBuilder.miteredRail(
			"header.main",
			profile,
			new Vec3d(0, y, 0),
			new Vec3d(layout.width(), y, 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		));
	}
}
