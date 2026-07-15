package dev.aperture.core.component;

import java.util.Map;

public record SillComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public SillComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.SILL;
	}

	public String profileId() {
		return property("profile", "");
	}

	public static SillComponent of(String id, String profileId) {
		return new SillComponent(ComponentRef.of(id), ComponentProperties.of("profile", profileId));
	}
}
