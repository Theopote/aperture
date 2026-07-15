package dev.aperture.opening.geometry.pipeline.panel;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.geometry.kinematic.PanelKinematics;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;

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
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningParameters parameters = context.openingParameters();
		if (!parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		ProfileCurve sashProfile = context.resolvedProfiles().panelProfile().orElseThrow().curve();
		List<PanelCellLayout> cells = PanelLayoutPlanner.plan(parameters, layout);
		for (PanelCellLayout cell : cells) {
			emitPanelCell(target, sashProfile, layout, cell);
		}
	}

	private static void emitPanelCell(
		GeometryCompilationTarget target,
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

		emitExtrudedRail(target, prefix + ".bottom", sashProfile, panelTransform,
			new Vec3d(originX, originY, 0),
			new Vec3d(originX + panelWidth, originY, 0));
		emitExtrudedRail(target, prefix + ".top", sashProfile, panelTransform,
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			new Vec3d(originX + panelWidth, originY + panelHeight - layout.sashFace(), 0));
		emitExtrudedRail(target, prefix + ".left", sashProfile, panelTransform,
			new Vec3d(originX, originY + layout.sashFace(), 0),
			new Vec3d(originX, originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ());
		emitExtrudedRail(target, prefix + ".right", sashProfile, panelTransform,
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + layout.sashFace(), 0),
			new Vec3d(originX + panelWidth - layout.sashFace(), originY + panelHeight - layout.sashFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ());

		double innerWidth = panelWidth - layout.sashFace() * 2;
		if (innerWidth <= 0) {
			return;
		}

		if (cell.glassHeight() > 0) {
			double glassHeight = Math.min(cell.glassHeight(), panelHeight - layout.sashFace() * 2);
			target.emitSolid(
				prefix + ".glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT,
				ShapeRecipes.box(new BoundingBox(
					new Vec3d(originX + layout.sashFace(), cell.glassBottomY(), 0),
					new Vec3d(originX + layout.sashFace() + innerWidth, cell.glassBottomY() + glassHeight, PANEL_GLAZING_DEPTH)
				)),
				panelTransform
			);
		}

		if (cell.solidHeight() > layout.sashFace()) {
			double infillHeight = cell.solidHeight() - layout.sashFace();
			target.emitSolid(
				prefix + ".infill",
				"frame",
				GeometryLayer.OPAQUE,
				ShapeRecipes.box(new BoundingBox(
					new Vec3d(originX + layout.sashFace(), originY + layout.sashFace(), 0),
					new Vec3d(originX + layout.sashFace() + innerWidth, originY + layout.sashFace() + infillHeight, layout.frameDepth() * 0.6)
				)),
				panelTransform
			);
		}
	}

	private static void emitExtrudedRail(
		GeometryCompilationTarget target,
		String componentPath,
		ProfileCurve profile,
		Transform3d transform,
		Vec3d pathStart,
		Vec3d pathEnd
	) {
		emitExtrudedRail(
			target,
			componentPath,
			profile,
			transform,
			pathStart,
			pathEnd,
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ()
		);
	}

	private static void emitExtrudedRail(
		GeometryCompilationTarget target,
		String componentPath,
		ProfileCurve profile,
		Transform3d transform,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		ShapeRecipe shape = ShapeRecipes.extrudeLinear(profile, pathStart, pathEnd, profileU, profileV);
		target.emitSolid(componentPath, "frame", GeometryLayer.OPAQUE, shape, transform);
	}
}
