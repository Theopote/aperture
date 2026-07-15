package dev.aperture.opening.resolve;

import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.ResolvedProfiles;

import java.util.Optional;

/**
 * Resolves and scales catalog profiles from a {@link GenerationContext}.
 * Profile resolution belongs to the parameter layer, not geometry steps.
 */
public final class ProfileResolver {
	private ProfileResolver() {
	}

	public static ResolvedProfiles resolve(GenerationContext context) {
		ProfileDefinition frame = context.scaledFrameProfile();
		Optional<ProfileDefinition> panel = context.hasComponent("panel")
			? Optional.of(context.scaledPanelProfile())
			: Optional.empty();
		return new ResolvedProfiles(frame, panel);
	}
}
