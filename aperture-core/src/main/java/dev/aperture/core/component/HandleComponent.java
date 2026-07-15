package dev.aperture.core.component;

import java.util.Map;

/**
 * Door or operable-panel latch hardware.
 */
public record HandleComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public HandleComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.HANDLE;
	}

	public static HandleComponent of(String id) {
		return new HandleComponent(ComponentRef.of(id), Map.of());
	}
}
