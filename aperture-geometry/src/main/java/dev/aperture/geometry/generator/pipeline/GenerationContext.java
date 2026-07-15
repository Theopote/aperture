package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.profile.ProfileDefinition;

import java.util.Map;

/**
 * Inputs available to each generation stage in a pipeline run.
 */
public final class GenerationContext {
	private final OpeningTypeDefinition definition;
	private final ParameterSet parameters;
	private final ProfileCatalogRegistry profiles;

	public GenerationContext(
		OpeningTypeDefinition definition,
		ParameterSet parameters,
		ProfileCatalogRegistry profiles
	) {
		this.definition = definition;
		this.parameters = parameters;
		this.profiles = profiles;
	}

	public OpeningTypeDefinition definition() {
		return definition;
	}

	public ParameterSet parameters() {
		return parameters;
	}

	public ProfileCatalogRegistry profiles() {
		return profiles;
	}

	public double requireLength(String name) {
		return parameters.requireLength(name);
	}

	public int requireCount(String name) {
		return parameters.requireCount(name);
	}

	public String componentProfileId(String componentName) {
		Object raw = definition.components().get(componentName);
		if (!(raw instanceof Map<?, ?> component)) {
			throw new IllegalStateException("Missing component definition: " + componentName);
		}
		Object profileId = component.get("profile");
		if (profileId == null) {
			throw new IllegalStateException("Component '" + componentName + "' has no profile binding");
		}
		return profileId.toString();
	}

	public ProfileDefinition requireProfile(String profileId) {
		return profiles.requireById(profileId);
	}

	public ProfileDefinition requireComponentProfile(String componentName) {
		return requireProfile(componentProfileId(componentName));
	}
}
