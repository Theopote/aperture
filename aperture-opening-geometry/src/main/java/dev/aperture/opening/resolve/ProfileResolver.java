package dev.aperture.opening.resolve;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.PanelComponent;
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
		ProfileDefinition frame = context.components().frame()
			.map(frameComponent -> context.scaledFrameProfile(frameComponent.profileId()))
			.orElseThrow(() -> missingComponent(ComponentKind.FRAME));
		Optional<ProfileDefinition> panel = context.components().panel()
			.map(panelComponent -> context.scaledPanelProfile(panelComponent.profileId()));
		return new ResolvedProfiles(frame, panel);
	}

	private static IllegalStateException missingComponent(ComponentKind kind) {
		return new IllegalStateException("Missing component definition: " + kind.jsonKey());
	}
}
