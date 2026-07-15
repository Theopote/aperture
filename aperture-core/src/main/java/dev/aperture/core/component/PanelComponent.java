package dev.aperture.core.component;

import java.util.LinkedHashMap;
import java.util.Map;

public record PanelComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public PanelComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.PANEL;
	}

	public String profileId() {
		return property("profile", "");
	}

	public String hinge() {
		return property("hinge", "left");
	}

	public static PanelComponent of(String id, String profileId, String hinge) {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("profile", profileId);
		props.put("hinge", hinge);
		return new PanelComponent(ComponentRef.of(id), props);
	}
}
