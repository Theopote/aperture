package dev.aperture.opening.resolve;

import dev.aperture.core.component.DividerComponent;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.HeaderComponent;
import dev.aperture.core.component.OpeningComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.component.SillComponent;
import dev.aperture.core.component.TrimComponent;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;

/**
 * Resolves and scales catalog profiles for one bound component instance.
 */
public final class ComponentProfileResolver {
	private ComponentProfileResolver() {
	}

	public static ProfileDefinition resolve(GenerationContext context, OpeningComponent component) {
		return switch (component.kind()) {
			case PANEL -> context.scaledPanelProfile(profileId(context, component));
			case FRAME, HEADER, SILL, TRIM, DIVIDER ->
				context.scaledFrameProfile(profileId(context, component));
			default -> throw new IllegalArgumentException(
				"Component kind has no profile: " + component.kind()
			);
		};
	}

	public static ProfileCurve curve(GenerationContext context, OpeningComponent component) {
		return resolve(context, component).curve();
	}

	private static String profileId(GenerationContext context, OpeningComponent component) {
		return switch (component) {
			case FrameComponent frame -> requireProfileId(frame.profileId(), frame.ref().id());
			case PanelComponent panel -> requireProfileId(panel.profileId(), panel.ref().id());
			case HeaderComponent header -> requireProfileId(header.profileId(), header.ref().id());
			case SillComponent sill -> requireProfileId(sill.profileId(), sill.ref().id());
			case TrimComponent trim -> requireProfileId(trim.profileId(), trim.ref().id());
			case DividerComponent divider -> divider.property(
				"profile",
				context.components().frame()
					.map(FrameComponent::profileId)
					.orElseThrow(() -> missingProfile(divider.ref().id()))
			);
			default -> throw new IllegalArgumentException(
				"Component kind has no profile: " + component.kind()
			);
		};
	}

	private static String requireProfileId(String profileId, String componentId) {
		if (profileId == null || profileId.isBlank()) {
			throw missingProfile(componentId);
		}
		return profileId;
	}

	private static IllegalStateException missingProfile(String componentId) {
		return new IllegalStateException("Missing profile for component: " + componentId);
	}
}
