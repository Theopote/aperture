package dev.aperture.opening.resolve;

import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;

/**
 * Parameter resolution layer: scales profiles and derives layout from {@link GenerationContext#parameters()}.
 */
public final class OpeningParameterResolver {
	public ResolvedOpening resolve(GenerationContext context) {
		var profiles = ProfileResolver.resolve(context);
		OpeningLayout layout = OpeningLayout.from(context, profiles);
		return new ResolvedOpening(context, profiles, layout);
	}
}
