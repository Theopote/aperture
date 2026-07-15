package dev.aperture.opening.geometry.pipeline.mullion;

import dev.aperture.core.component.MullionComponent;
import dev.aperture.core.parametric.ParameterRef;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.frame.FrameRailBuilder;

/**
 * Generates structural mullion rails for one mullion component instance.
 */
public final class MullionGenerator implements ComponentPipelineStep {
	private final MullionComponent component;

	public MullionGenerator(MullionComponent component) {
		this.component = component;
	}

	@Override
	public MullionComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningLayout layout = context.layout();
		OpeningParameters parameters = context.openingParameters();
		ProfileCurve profile = context.profileCurveFor(component);
		String root = component.ref().id();

		switch (resolveSource(component.source())) {
			case COLS -> emitVerticalMullions(target, layout, profile, parameters, root, parameters.cols() - 1);
			case ROWS -> emitHorizontalMullions(target, layout, profile, parameters, root, parameters.rows() - 1);
			case MULLIONS -> emitVerticalMullions(target, layout, profile, parameters, root, parameters.mullions());
			case ALL -> {
				emitVerticalMullions(target, layout, profile, parameters, root, verticalCount(parameters));
				emitHorizontalMullions(target, layout, profile, parameters, root, parameters.rows() - 1);
			}
		}
	}

	private static int verticalCount(OpeningParameters parameters) {
		return parameters.cols() > 1 ? parameters.cols() - 1 : parameters.mullions();
	}

	private static MullionSource resolveSource(String source) {
		if (source == null || source.isBlank()) {
			return MullionSource.MULLIONS;
		}
		if (ParameterRef.isReference(source)) {
			return switch (ParameterRef.parse(source).name()) {
				case "cols" -> MullionSource.COLS;
				case "rows" -> MullionSource.ROWS;
				case "mullions" -> MullionSource.MULLIONS;
				default -> MullionSource.ALL;
			};
		}
		return switch (source) {
			case String value when value.endsWith(":cols") -> MullionSource.COLS;
			case String value when value.endsWith(":rows") -> MullionSource.ROWS;
			case String value when value.endsWith(":mullions") -> MullionSource.MULLIONS;
			default -> MullionSource.ALL;
		};
	}

	private static void emitVerticalMullions(
		GeometryCompilationTarget target,
		OpeningLayout layout,
		ProfileCurve profile,
		OpeningParameters parameters,
		String root,
		int count
	) {
		if (count <= 0) {
			return;
		}
		for (int i = 1; i <= count; i++) {
			double t = (double) i / (count + 1);
			double x = layout.frameFace() + layout.innerWidth() * t - layout.frameFace() / 2.0;
			String path = parameters.cols() > 1
				? ComponentPaths.join(root, "vertical." + i)
				: ComponentPaths.join(root, "mullion." + i);
			FrameRailBuilder.emitMiteredRail(
				target,
				path,
				profile,
				new Vec3d(x, layout.frameFace(), 0),
				new Vec3d(x, layout.height() - layout.frameFace(), 0),
				FrameRailBuilder.axisX(),
				FrameRailBuilder.axisZ()
			);
		}
	}

	private static void emitHorizontalMullions(
		GeometryCompilationTarget target,
		OpeningLayout layout,
		ProfileCurve profile,
		OpeningParameters parameters,
		String root,
		int count
	) {
		if (count <= 0) {
			return;
		}
		for (int i = 1; i <= count; i++) {
			double t = (double) i / (count + 1);
			double y = layout.frameFace() + layout.innerHeight() * t - layout.frameFace() / 2.0;
			FrameRailBuilder.emitMiteredRail(
				target,
				ComponentPaths.join(root, "horizontal." + i),
				profile,
				new Vec3d(layout.frameFace(), y, 0),
				new Vec3d(layout.width() - layout.frameFace(), y, 0),
				FrameRailBuilder.axisY(),
				FrameRailBuilder.axisZ()
			);
		}
	}

	private enum MullionSource {
		MULLIONS,
		COLS,
		ROWS,
		ALL
	}
}
