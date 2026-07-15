package dev.aperture.opening.geometry.pipeline.panel;

import dev.aperture.core.component.PanelComponent;
import dev.aperture.opening.resolve.ComponentPropertyResolver;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.geometry.kinematic.PanelKinematics;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;
import dev.aperture.geometry.profile.ProfileCurve;

import java.util.List;

/**
 * Generates operable panel sash geometry for one panel component instance.
 */
public final class PanelGenerator implements ComponentPipelineStep {
	private static final double PANEL_GLAZING_DEPTH = 8;
	private final PanelComponent component;

	public PanelGenerator(PanelComponent component) {
		this.component = component;
	}

	@Override
	public PanelComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningParameters parameters = context.openingParameters();
		if (!parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		ProfileCurve sashProfile = context.profileCurveFor(component);
		List<PanelCellLayout> cells = PanelLayoutPlanner.plan(parameters, layout, resolveHingeSide(context));
		for (PanelCellLayout cell : cells) {
			emitPanelCell(target, sashProfile, layout, cell);
		}
	}

	private void emitPanelCell(
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

		String prefix = pathPrefix(cell);
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

	private String resolveHingeSide(OpeningPipelineContext context) {
		return ComponentPropertyResolver.panelHinge(context.source(), component.hinge());
	}

	private String pathPrefix(PanelCellLayout cell) {
		String root = component.ref().id();
		return cell.panelCount() == 1 ? root : ComponentPaths.join(root, String.valueOf(cell.index()));
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
