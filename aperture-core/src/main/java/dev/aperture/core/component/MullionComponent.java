package dev.aperture.core.component;

import java.util.Map;

/**
 * Structural mullion rails between glazing cells or fixed lights.
 */
public record MullionComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public MullionComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.MULLION;
	}

	public String source() {
		return property("source", "parameter:mullions");
	}

	public static MullionComponent fromSource(String id, String source) {
		return new MullionComponent(ComponentRef.of(id), ComponentProperties.of("source", source));
	}
}
