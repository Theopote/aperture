package dev.aperture.geometry.pipeline.panel;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.kinematic.PanelKinematics;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.pipeline.OpeningParameters;
import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

/**
 * Generates operable panel sash geometry with hinge kinematics.
 */
public final class PanelGenerator implements PipelineStep {
	public static final String STEP_ID = "panel";
	private static final double PANEL_GLAZING_DEPTH = 8;

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningParameters parameters = context.openingParameters();
		if (!parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		ProfileCurve sashProfile = context.resolvedProfiles().panelProfile().orElseThrow().curve();
		double panelWidth = layout.innerWidth();
		double panelHeight = layout.innerHeight();
		if (panelWidth <= 0 || panelHeight <= 0) {
			return;
		}

		Transform3d panelTransform = PanelKinematics.solve(
			parameters.panelHinge(),
			layout.frameFace(),
			layout.width(),
			layout.height(),
			parameters.openAngleDegrees()
		);
		double originX = layout.frameFace();
		double originY = layout.frameFace();

		assembly.addSolid(extrudedRail("panel.bottom", sashProfile, panelTransform,
			new Vec3d(originX, originY, 0),
			new Vec3d(originX + panelWidth, originY, 0)));
		assembly.addSolid(extrudedRail("panel.top", sashProfile, panelTransform,
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			new Vec3d(originX + panelWidth, originY + panelHeight - layout.sashFace(), 0)));
		assembly.addSolid(extrudedRail("panel.left", sashProfile, panelTransform,
			new Vec3d(originX, originY + layout.sashFace(), 0),
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ()));
		assembly.addSolid(extrudedRail("panel.right", sashProfile, panelTransform,
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + layout.sashFace(), 0),
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ()));

		double innerWidth = panelWidth - layout.sashFace() * 2;
		double innerHeight = panelHeight - layout.sashFace() * 2;
		if (innerWidth > 0 && innerHeight > 0) {
			assembly.addSolid(GeometrySolid.of(
				"panel.glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT_GLASS,
				new dev.aperture.geometry.shape.BoxShape(new BoundingBox(
					new Vec3d(originX + layout.sashFace(), originY + layout.sashFace(), 0),
					new Vec3d(originX + layout.sashFace() + innerWidth, originY + layout.sashFace() + innerHeight, PANEL_GLAZING_DEPTH)
				)),
				panelTransform
			));
		}
	}

	private static GeometrySolid extrudedRail(
		String componentPath,
		ProfileCurve profile,
		Transform3d transform,
		Vec3d pathStart,
		Vec3d pathEnd
	) {
		return extrudedRail(
			componentPath,
			profile,
			transform,
			pathStart,
			pathEnd,
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ()
		);
	}

	private static GeometrySolid extrudedRail(
		String componentPath,
		ProfileCurve profile,
		Transform3d transform,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		SolidShape shape = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
		return GeometrySolid.of(componentPath, "frame", GeometryLayer.OPAQUE_FRAME, shape, transform);
	}
}
