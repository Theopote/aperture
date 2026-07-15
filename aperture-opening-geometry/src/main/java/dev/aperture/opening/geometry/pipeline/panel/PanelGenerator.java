package dev.aperture.opening.geometry.pipeline.panel;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.opening.geometry.kinematic.PanelKinematics;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

import java.util.List;

/**
 * Generates operable panel sash geometry with multi-panel layout and glass ratio support.
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
		List<PanelCellLayout> cells = PanelLayoutPlanner.plan(parameters, layout);
		for (PanelCellLayout cell : cells) {
			emitPanelCell(assembly, sashProfile, layout, cell);
		}
	}

	private static void emitPanelCell(
		GeometryAssemblyBuilder assembly,
		ProfileCurve sashProfile,
		OpeningLayout layout,
		PanelCellLayout cell
	) {
		if (cell.width() <= 0 || cell.height() <= 0) {
			return;
		}

		Transform3d panelTransform = cell.operable()
			? PanelKinematics.solveAtHinge(
				cell.hingeSide(),
				new Vec3d(cell.hingeX(), cell.originY(), 0),
				cell.openAngleDegrees()
			)
			: Transform3d.identity();

		String prefix = cell.pathPrefix();
		double originX = cell.originX();
		double originY = cell.originY();
		double panelWidth = cell.width();
		double panelHeight = cell.height();

		assembly.addSolid(extrudedRail(prefix + ".bottom", sashProfile, panelTransform,
			new Vec3d(originX, originY, 0),
			new Vec3d(originX + panelWidth, originY, 0)));
		assembly.addSolid(extrudedRail(prefix + ".top", sashProfile, panelTransform,
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			new Vec3d(originX + panelWidth, originY + panelHeight - layout.sashFace(), 0)));
		assembly.addSolid(extrudedRail(prefix + ".left", sashProfile, panelTransform,
			new Vec3d(originX, originY + layout.sashFace(), 0),
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ()));
		assembly.addSolid(extrudedRail(prefix + ".right", sashProfile, panelTransform,
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + layout.sashFace(), 0),
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ()));

		double innerWidth = panelWidth - layout.sashFace() * 2;
		if (innerWidth <= 0) {
			return;
		}

		if (cell.glassHeight() > 0) {
			double glassHeight = Math.min(cell.glassHeight(), panelHeight - layout.sashFace() * 2);
			assembly.addSolid(GeometrySolid.of(
				prefix + ".glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT,
				new dev.aperture.geometry.shape.BoxShape(new BoundingBox(
					new Vec3d(originX + layout.sashFace(), cell.glassBottomY(), 0),
					new Vec3d(originX + layout.sashFace() + innerWidth, cell.glassBottomY() + glassHeight, PANEL_GLAZING_DEPTH)
				)),
				panelTransform
			));
		}

		if (cell.solidHeight() > layout.sashFace()) {
			double infillHeight = cell.solidHeight() - layout.sashFace();
			assembly.addSolid(GeometrySolid.of(
				prefix + ".infill",
				"frame",
				GeometryLayer.OPAQUE,
				new dev.aperture.geometry.shape.BoxShape(new BoundingBox(
					new Vec3d(originX + layout.sashFace(), originY + layout.sashFace(), 0),
					new Vec3d(originX + layout.sashFace() + innerWidth, originY + layout.sashFace() + infillHeight, layout.frameDepth() * 0.6)
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
		return GeometrySolid.of(componentPath, "frame", GeometryLayer.OPAQUE, shape, transform);
	}
}
