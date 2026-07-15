package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.geometry.profile.ProfileScaler;

import java.util.Map;
import java.util.Optional;

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

	public double angleDegrees(String name, double defaultDegrees) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.ANGLE)
			.map(value -> ((ParameterValue.AngleValue) value).degrees())
			.orElse(defaultDegrees);
	}

	public boolean hasComponent(String componentName) {
		return definition.components().containsKey(componentName);
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

	public String componentString(String componentName, String key, String defaultValue) {
		Object raw = definition.components().get(componentName);
		if (!(raw instanceof Map<?, ?> component)) {
			return defaultValue;
		}
		Object value = component.get(key);
		return value == null ? defaultValue : value.toString();
	}

	public ProfileDefinition requireProfile(String profileId) {
		return profiles.requireById(profileId);
	}

	public ProfileDefinition requireComponentProfile(String componentName) {
		return requireProfile(componentProfileId(componentName));
	}

	public Optional<Double> optionalLength(String name) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}

	public ProfileDefinition scaledFrameProfile() {
		return scaleProfile(requireComponentProfile("frame"));
	}

	public ProfileDefinition scaledPanelProfile() {
		ProfileDefinition base = requireComponentProfile("panel");
		double sashWidth = optionalLength("panel_width")
			.orElse(requireLength("frame_width") * 0.75);
		return ProfileScaler.scaleToFrameWidth(base, sashWidth);
	}

	private ProfileDefinition scaleProfile(ProfileDefinition base) {
		double targetWidth = requireLength("frame_width");
		double targetDepth = optionalLength("frame_depth")
			.orElse(base.bounds().depth() * (targetWidth / base.bounds().width()));
		return ProfileScaler.scaleToFrameSize(base, targetWidth, targetDepth);
	}
}
