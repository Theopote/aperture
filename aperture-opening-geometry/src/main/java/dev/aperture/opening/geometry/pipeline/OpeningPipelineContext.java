package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.OpeningComponent;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.opening.resolve.ComponentProfileResolver;
import dev.aperture.opening.resolve.OpeningParameterResolver;
import dev.aperture.opening.resolve.ResolvedOpening;

import java.util.Objects;

/**
 * Geometry-step context with pre-resolved parameters, profiles, and layout.
 */
public final class OpeningPipelineContext {
	private final ResolvedOpening resolved;

	public OpeningPipelineContext(ResolvedOpening resolved) {
		this.resolved = Objects.requireNonNull(resolved, "resolved");
	}

	public static OpeningPipelineContext from(GenerationContext source) {
		return new OpeningPipelineContext(new OpeningParameterResolver().resolve(source));
	}

	public static OpeningPipelineContext from(ResolvedOpening resolved) {
		return new OpeningPipelineContext(resolved);
	}

	public GenerationContext source() {
		return resolved.context();
	}

	public OpeningTypeDefinition definition() {
		return resolved.context().definition();
	}

	public dev.aperture.core.parameter.ParameterSet parameters() {
		return resolved.context().parameters();
	}

	public dev.aperture.geometry.profile.ProfileCatalogRegistry profileCatalog() {
		return resolved.context().profiles();
	}

	public OpeningParameters openingParameters() {
		return resolved.parameters();
	}

	public ResolvedProfiles resolvedProfiles() {
		return resolved.profiles();
	}

	public OpeningLayout layout() {
		return resolved.layout();
	}

	public dev.aperture.geometry.profile.ProfileDefinition scaledFrameProfile() {
		return resolved.context().scaledFrameProfile();
	}

	public dev.aperture.geometry.profile.ProfileDefinition scaledPanelProfile() {
		return resolved.context().scaledPanelProfile();
	}

	public boolean hasComponent(ComponentKind kind) {
		return resolved.context().hasComponent(kind);
	}

	public boolean hasComponentId(String componentId) {
		return resolved.context().components().findById(componentId).isPresent();
	}

	public ProfileDefinition profileFor(OpeningComponent component) {
		return ComponentProfileResolver.resolve(resolved.context(), component);
	}

	public ProfileCurve profileCurveFor(OpeningComponent component) {
		return ComponentProfileResolver.curve(resolved.context(), component);
	}
}
