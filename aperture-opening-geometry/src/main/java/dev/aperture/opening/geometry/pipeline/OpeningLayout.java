package dev.aperture.opening.geometry.pipeline;

import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.ResolvedProfiles;

/**
 * Derived layout metrics shared across frame, panel, glass, and accessory generators.
 */
public record OpeningLayout(
	double width,
	double height,
	double frameFace,
	double frameDepth,
	double innerWidth,
	double innerHeight,
	double sashFace
) {
	public static OpeningLayout from(GenerationContext context, ResolvedProfiles profiles) {
		double width = context.parameters().requireLength("width");
		double height = context.parameters().requireLength("height");
		double frameFace = profiles.frame().bounds().width();
		double frameDepth = profiles.frame().bounds().depth();
		double sashFace = profiles.panelProfile()
			.map(profile -> profile.bounds().width())
			.orElse(frameFace * 0.75);
		double innerWidth = width - frameFace * 2;
		double innerHeight = height - frameFace * 2;
		return new OpeningLayout(
			width,
			height,
			frameFace,
			frameDepth,
			innerWidth,
			innerHeight,
			sashFace
		);
	}
}
