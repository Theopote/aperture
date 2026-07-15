package dev.aperture.opening.geometry.generator.pipeline;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.geometry.profile.ProfileScaler;

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

	public ComponentAssembly components() {
		return definition.components();
	}

	public double requireLength(String name) {
		return parameters.requireLength(name);
	}

	public int requireCount(String name) {
		return parameters.requireCount(name);
	}

	public double angleDegrees(String name, double defaultDegrees) {
		return parameters.angleOrDefault(name, defaultDegrees);
	}

	public boolean hasComponent(ComponentKind kind) {
		return components().has(kind);
	}

	public ProfileDefinition requireProfile(String profileId) {
		return profiles.requireById(profileId);
	}

	public Optional<Double> optionalLength(String name) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}

	public ProfileDefinition scaledFrameProfile() {
		return scaledFrameProfile(requireFrameProfileId());
	}

	public ProfileDefinition scaledFrameProfile(String profileId) {
		return scaleProfile(requireProfile(profileId));
	}

	public ProfileDefinition scaledPanelProfile() {
		return scaledPanelProfile(requirePanelProfileId());
	}

	public ProfileDefinition scaledPanelProfile(String profileId) {
		ProfileDefinition base = requireProfile(profileId);
		double sashWidth = optionalLength("panel_width")
			.orElse(requireLength("frame_width") * 0.75);
		return ProfileScaler.scaleToFrameWidth(base, sashWidth);
	}

	private String requireFrameProfileId() {
		return components().frame()
			.map(FrameComponent::profileId)
			.filter(profileId -> !profileId.isBlank())
			.orElseThrow(() -> missingComponent(ComponentKind.FRAME));
	}

	private String requirePanelProfileId() {
		return components().panel()
			.map(PanelComponent::profileId)
			.filter(profileId -> !profileId.isBlank())
			.orElseThrow(() -> missingComponent(ComponentKind.PANEL));
	}

	private ProfileDefinition scaleProfile(ProfileDefinition base) {
		double targetWidth = requireLength("frame_width");
		double targetDepth = optionalLength("frame_depth")
			.orElse(optionalLength("thickness")
				.orElse(base.bounds().depth() * (targetWidth / base.bounds().width())));
		return ProfileScaler.scaleToFrameSize(base, targetWidth, targetDepth);
	}

	private static IllegalStateException missingComponent(ComponentKind kind) {
		return new IllegalStateException("Missing component definition: " + kind.jsonKey());
	}
}
