package dev.aperture.core.component;

import java.util.Map;

public record TrimComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public TrimComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.TRIM;
	}

	public static TrimComponent of(String id, String profileId) {
		return new TrimComponent(ComponentRef.of(id), ComponentProperties.of("profile", profileId));
	}
}
