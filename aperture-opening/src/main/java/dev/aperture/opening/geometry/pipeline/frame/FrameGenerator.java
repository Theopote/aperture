package dev.aperture.opening.geometry.pipeline.frame;

import dev.aperture.core.component.FrameComponent;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Generates the outer frame rails with corner miter CSG for one frame component instance.
 */
public final class FrameGenerator implements ComponentPipelineStep {
	private final FrameComponent component;

	public FrameGenerator(FrameComponent component) {
		this.component = component;
	}

	@Override
	public FrameComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		String root = component.ref().id();
		OpeningLayout layout = context.layout();
		ProfileCurve profile = context.profileCurveFor(component);

		FrameRailBuilder.emitMiteredRail(
			target,
			ComponentPaths.join(root, "bottom"),
			profile,
			new Vec3d(0, 0, 0),
			new Vec3d(layout.width(), 0, 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT)
		);
		FrameRailBuilder.emitMiteredRail(
			target,
			ComponentPaths.join(root, "top"),
			profile,
			new Vec3d(0, layout.height() - layout.frameFace(), 0),
			new Vec3d(layout.width(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		);
		FrameRailBuilder.emitMiteredRail(
			target,
			ComponentPaths.join(root, "left"),
			profile,
			new Vec3d(0, layout.frameFace(), 0),
			new Vec3d(0, layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT)
		);
		FrameRailBuilder.emitMiteredRail(
			target,
			ComponentPaths.join(root, "right"),
			profile,
			new Vec3d(layout.width() - layout.frameFace(), layout.frameFace(), 0),
			new Vec3d(layout.width() - layout.frameFace(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		);

		target.setCutVolume(BoundingBox.fromSize(layout.width(), layout.height(), 200));
	}
}
