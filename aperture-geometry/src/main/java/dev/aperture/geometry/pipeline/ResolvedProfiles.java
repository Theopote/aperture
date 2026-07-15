package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.profile.ProfileDefinition;

import java.util.Optional;

/**
 * Catalog profiles resolved and scaled for the current opening instance.
 */
public record ResolvedProfiles(
	ProfileDefinition frame,
	Optional<ProfileDefinition> panelProfile
) {
}
