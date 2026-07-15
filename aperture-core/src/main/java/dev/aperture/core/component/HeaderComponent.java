package dev.aperture.core.component;

import java.util.Map;

public record HeaderComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public HeaderComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.HEADER;
	}

	public String profileId() {
		return property("profile", "");
	}

	public static HeaderComponent of(String id, String profileId) {
		return new HeaderComponent(ComponentRef.of(id), ComponentProperties.of("profile", profileId));
	}
}
