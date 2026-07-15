package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
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
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.ANGLE)
			.map(value -> ((ParameterValue.AngleValue) value).degrees())
			.orElse(defaultDegrees);
	}

	public boolean hasComponent(ComponentKind kind) {
		return components().has(kind);
	}

	public boolean hasComponent(String legacyKey) {
		return components().hasLegacyKey(legacyKey);
	}

	public String componentProfileId(String legacyKey) {
		return switch (legacyKey) {
			case "frame" -> components().frame()
				.map(FrameComponent::profileId)
				.orElseThrow(() -> missingComponent("frame"));
			case "panel" -> components().panel()
				.map(PanelComponent::profileId)
				.orElseThrow(() -> missingComponent("panel"));
			default -> components().findById(legacyKey)
				.map(component -> component.property("profile", ""))
				.filter(profile -> !profile.isBlank())
				.orElseThrow(() -> missingComponent(legacyKey));
		};
	}

	public String componentString(String legacyKey, String key, String defaultValue) {
		Optional<PanelComponent> panel = components().panel();
		if ("panel".equals(legacyKey) && "hinge".equals(key) && panel.isPresent()) {
			String hingeSide = parameters.get("hinge_side")
				.filter(value -> value.type() == ParameterType.ENUM)
				.map(value -> ((ParameterValue.EnumValue) value).value())
				.orElse(panel.get().hinge());
			return hingeSide;
		}
		return switch (legacyKey) {
			case "panel" -> components().panel()
				.map(panelComponent -> panelComponent.property(key, defaultValue))
				.orElse(defaultValue);
			case "glazing" -> components().glass()
				.map(glass -> glass.property(key, defaultValue))
				.orElse(defaultValue);
			default -> components().findById(legacyKey)
				.map(component -> component.property(key, defaultValue))
				.orElse(defaultValue);
		};
	}

	public ProfileDefinition requireProfile(String profileId) {
		return profiles.requireById(profileId);
	}

	public ProfileDefinition requireComponentProfile(String legacyKey) {
		return requireProfile(componentProfileId(legacyKey));
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
			.orElse(optionalLength("thickness")
				.orElse(base.bounds().depth() * (targetWidth / base.bounds().width())));
		return ProfileScaler.scaleToFrameSize(base, targetWidth, targetDepth);
	}

	private static IllegalStateException missingComponent(String name) {
		return new IllegalStateException("Missing component definition: " + name);
	}
}
