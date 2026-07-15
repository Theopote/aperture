package dev.aperture.opening.geometry.pipeline.header;

import dev.aperture.core.component.HeaderComponent;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;

/**
 * Generates the structural header / head member for one header component instance.
 */
public final class HeaderGenerator implements ComponentPipelineStep {
	private final HeaderComponent component;

	public HeaderGenerator(HeaderComponent component) {
		this.component = component;
	}

	@Override
	public HeaderComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningLayout layout = context.layout();
		ProfileCurve profile = context.resolvedProfiles().frame().curve();
		double y = layout.height() - layout.frameFace();
		FrameRailBuilder.emitMiteredRail(
			target,
			ComponentPaths.join(component.ref().id(), "main"),
			profile,
			new Vec3d(0, y, 0),
			new Vec3d(layout.width(), y, 0),
			FrameRailBuilder.axisY(),
			FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT)
		);
	}
}
