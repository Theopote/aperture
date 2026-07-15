package dev.aperture.core.component;

import java.util.Map;

public record HardwareComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public HardwareComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.HARDWARE;
	}

	public String hardwareType() {
		return property("type", "generic");
	}

	public static HardwareComponent of(String id, String type) {
		return new HardwareComponent(ComponentRef.of(id), ComponentProperties.of("type", type));
	}
}
