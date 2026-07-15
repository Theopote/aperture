package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.kinematic.PanelKinematics;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.geometry.shape.SolidShape;

/**
 * Generates operable panel sash geometry with hinge kinematics.
 */
public final class PanelStage implements GenerationStage {
	private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);
	private static final Vec3d AXIS_Z = new Vec3d(0, 0, 1);
	private static final double PANEL_GLAZING_DEPTH = 8;

	@Override
	public String id() {
		return "panel";
	}

	@Override
	public void contribute(GenerationContext context, GeometryAssemblyBuilder builder) {
		if (!context.hasComponent("panel")) {
			return;
		}

		double width = context.requireLength("width");
		double height = context.requireLength("height");
		double openAngle = context.angleDegrees("open_angle", 0);
		String hinge = context.componentString("panel", "hinge", "left");

		ProfileDefinition frameProfile = context.scaledFrameProfile();
		ProfileDefinition panelProfile = context.scaledPanelProfile();
		ProfileCurve sashProfile = panelProfile.curve();

		double frameFace = frameProfile.bounds().width();
		double sashFace = panelProfile.bounds().width();
		double panelWidth = width - frameFace * 2;
		double panelHeight = height - frameFace * 2;
		if (panelWidth <= 0 || panelHeight <= 0) {
			return;
		}

		Transform3d panelTransform = PanelKinematics.solve(hinge, frameFace, width, height, openAngle);
		double originX = frameFace;
		double originY = frameFace;

		builder.addSolid(extrudedRail(
			"panel.bottom",
			sashProfile,
			panelTransform,
			new Vec3d(originX, originY, 0),
			new Vec3d(originX + panelWidth, originY, 0),
			AXIS_Y,
			AXIS_Z
		));
		builder.addSolid(extrudedRail(
			"panel.top",
			sashProfile,
			panelTransform,
			new Vec3d(originX, originY + panelHeight - sashFace, 0),
			new Vec3d(originX + panelWidth, originY + panelHeight - sashFace, 0),
			AXIS_Y,
			AXIS_Z
		));
		builder.addSolid(extrudedRail(
			"panel.left",
			sashProfile,
			panelTransform,
			new Vec3d(originX, originY + sashFace, 0),
			new Vec3d(originX, originY + panelHeight - sashFace, 0),
			AXIS_X,
			AXIS_Z
		));
		builder.addSolid(extrudedRail(
			"panel.right",
			sashProfile,
			panelTransform,
			new Vec3d(originX + panelWidth - sashFace, originY + sashFace, 0),
			new Vec3d(originX + panelWidth - sashFace, originY + panelHeight - sashFace, 0),
			AXIS_X,
			AXIS_Z
		));

		double innerWidth = panelWidth - sashFace * 2;
		double innerHeight = panelHeight - sashFace * 2;
		if (innerWidth > 0 && innerHeight > 0) {
			builder.addSolid(GeometrySolid.of(
				"panel.glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT_GLASS,
				new dev.aperture.geometry.shape.BoxShape(new BoundingBox(
					new Vec3d(originX + sashFace, originY + sashFace, 0),
					new Vec3d(originX + sashFace + innerWidth, originY + sashFace + innerHeight, PANEL_GLAZING_DEPTH)
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
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		SolidShape shape = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
		return GeometrySolid.of(componentPath, "frame", GeometryLayer.OPAQUE_FRAME, shape, transform);
	}
}
