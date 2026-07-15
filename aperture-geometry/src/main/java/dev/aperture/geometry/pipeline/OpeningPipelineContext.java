package dev.aperture.geometry.pipeline;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.profile.ProfileDefinition;

import java.util.Objects;

/**
 * Mutable pipeline context flowing through Profile → Frame → Panel → Glass → Accessory.
 */
public final class OpeningPipelineContext {
	private final GenerationContext source;
	private OpeningParameters parameters;
	private ResolvedProfiles profiles;
	private OpeningLayout layout;

	public OpeningPipelineContext(GenerationContext source) {
		this.source = Objects.requireNonNull(source, "source");
	}

	public static OpeningPipelineContext from(GenerationContext source) {
		return new OpeningPipelineContext(source);
	}

	public OpeningTypeDefinition definition() {
		return source.definition();
	}

	public ParameterSet parameters() {
		return source.parameters();
	}

	public ProfileCatalogRegistry profileCatalog() {
		return source.profiles();
	}

	public GenerationContext source() {
		return source;
	}

	public OpeningParameters openingParameters() {
		if (parameters == null) {
			parameters = OpeningParameters.from(source);
		}
		return parameters;
	}

	public ResolvedProfiles resolvedProfiles() {
		requireProfilesResolved();
		return profiles;
	}

	public OpeningLayout layout() {
		requireProfilesResolved();
		if (layout == null) {
			layout = OpeningLayout.from(openingParameters(), profiles);
		}
		return layout;
	}

	public void setResolvedProfiles(ResolvedProfiles profiles) {
		this.profiles = Objects.requireNonNull(profiles, "profiles");
		this.layout = null;
	}

	public ProfileDefinition scaledFrameProfile() {
		return source.scaledFrameProfile();
	}

	public ProfileDefinition scaledPanelProfile() {
		return source.scaledPanelProfile();
	}

	public boolean hasComponent(String componentName) {
		return source.hasComponent(componentName);
	}

	private void requireProfilesResolved() {
		if (profiles == null) {
			throw new IllegalStateException("ProfileGenerator has not run yet");
		}
	}
}
