package dev.aperture.opening.resolve;

import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;

/**
 * Parameter resolution layer: merges schema defaults, scales profiles, derives layout.
 */
public final class OpeningParameterResolver {
	public ResolvedOpening resolve(GenerationContext context) {
		OpeningParameters parameters = OpeningParameters.from(context);
		var profiles = ProfileResolver.resolve(context);
		OpeningLayout layout = OpeningLayout.from(parameters, profiles);
		return new ResolvedOpening(context, parameters, profiles, layout);
	}
}
