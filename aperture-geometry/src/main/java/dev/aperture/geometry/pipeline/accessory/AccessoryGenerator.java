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
 * Generates mullions and curtain-wall grid dividers.
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

		emitVerticalMullions(assembly, layout, profile, parameters);
		emitHorizontalMullions(assembly, layout, profile, parameters);
	}

	private static void emitVerticalMullions(
		GeometryAssemblyBuilder assembly,
		OpeningLayout layout,
		ProfileCurve profile,
		OpeningParameters parameters
	) {
		int count = parameters.cols() > 1
			? parameters.cols() - 1
			: parameters.mullions();
		for (int i = 1; i <= count; i++) {
			double t = (double) i / (count + 1);
			double x = layout.frameFace() + layout.innerWidth() * t - layout.frameFace() / 2.0;
			String path = parameters.cols() > 1 ? "divider.vertical." + i : "frame.mullion." + i;
			assembly.addSolid(FrameRailBuilder.miteredRail(
				path,
				profile,
				new Vec3d(x, layout.frameFace(), 0),
				new Vec3d(x, layout.height() - layout.frameFace(), 0),
				FrameRailBuilder.axisX(),
				FrameRailBuilder.axisZ()
			));
		}
	}

	private static void emitHorizontalMullions(
		GeometryAssemblyBuilder assembly,
		OpeningLayout layout,
		ProfileCurve profile,
		OpeningParameters parameters
	) {
		if (parameters.rows() <= 1) {
			return;
		}
		int count = parameters.rows() - 1;
		for (int i = 1; i <= count; i++) {
			double t = (double) i / (count + 1);
			double y = layout.frameFace() + layout.innerHeight() * t - layout.frameFace() / 2.0;
			assembly.addSolid(FrameRailBuilder.miteredRail(
				"divider.horizontal." + i,
				profile,
				new Vec3d(layout.frameFace(), y, 0),
				new Vec3d(layout.width() - layout.frameFace(), y, 0),
				FrameRailBuilder.axisY(),
				FrameRailBuilder.axisZ()
			));
		}
	}
}
