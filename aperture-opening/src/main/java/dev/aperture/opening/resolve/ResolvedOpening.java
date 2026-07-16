package dev.aperture.opening.resolve;

import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.ResolvedProfiles;

/**
 * Fully resolved opening inputs after definition lookup and parameter merge.
 */
public record ResolvedOpening(
	GenerationContext context,
	ResolvedProfiles profiles,
	OpeningLayout layout
) {
}
