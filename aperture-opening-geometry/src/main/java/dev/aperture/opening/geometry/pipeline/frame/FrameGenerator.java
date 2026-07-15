package dev.aperture.opening.geometry.pipeline.frame;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.geometry.profile.ProfileCurve;

/**
 * Generates the outer frame rails with corner miter CSG.
 */
public final class FrameGenerator implements PipelineStep {
	public static final String STEP_ID = "frame";

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningLayout layout = context.layout();
		ProfileCurve profile = context.resolvedProfiles().frame().curve();

		assembly.addSolid(FrameRailBuilder.miteredRail(
			"frame.bottom",
			profile,
			new Vec3d(0, 0, 0),
			new Vec3d(layout.width(), 0, 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT)
		));
		assembly.addSolid(FrameRailBuilder.miteredRail(
			"frame.top",
			profile,
			new Vec3d(0, layout.height() - layout.frameFace(), 0),
			new Vec3d(layout.width(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		));
		assembly.addSolid(FrameRailBuilder.miteredRail(
			"frame.left",
			profile,
			new Vec3d(0, layout.frameFace(), 0),
			new Vec3d(0, layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT)
		));
		assembly.addSolid(FrameRailBuilder.miteredRail(
			"frame.right",
			profile,
			new Vec3d(layout.width() - layout.frameFace(), layout.frameFace(), 0),
			new Vec3d(layout.width() - layout.frameFace(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		));

		assembly.setCutVolume(BoundingBox.fromSize(layout.width(), layout.height(), 200));
	}
}
