package dev.aperture.opening.geometry.pipeline;

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
	public static OpeningLayout from(OpeningParameters parameters, ResolvedProfiles profiles) {
		double frameFace = profiles.frame().bounds().width();
		double frameDepth = profiles.frame().bounds().depth();
		double sashFace = profiles.panelProfile()
			.map(profile -> profile.bounds().width())
			.orElse(frameFace * 0.75);
		double innerWidth = parameters.width() - frameFace * 2;
		double innerHeight = parameters.height() - frameFace * 2;
		return new OpeningLayout(
			parameters.width(),
			parameters.height(),
			frameFace,
			frameDepth,
			innerWidth,
			innerHeight,
			sashFace
		);
	}
}
