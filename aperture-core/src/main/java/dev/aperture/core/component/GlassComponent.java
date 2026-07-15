package dev.aperture.core.component;

import java.util.Map;

public record GlassComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public GlassComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.GLASS;
	}

	public String systemId() {
		return property("system", "");
	}

	public static GlassComponent of(String id, String systemId) {
		return new GlassComponent(ComponentRef.of(id), ComponentProperties.of("system", systemId));
	}
}
