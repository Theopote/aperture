package dev.aperture.core.component;

import java.util.Map;

public record DecorationComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public DecorationComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.DECORATION;
	}

	public static DecorationComponent of(String id, String style) {
		return new DecorationComponent(ComponentRef.of(id), ComponentProperties.of("style", style));
	}
}
