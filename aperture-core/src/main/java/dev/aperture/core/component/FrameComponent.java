package dev.aperture.core.component;

import java.util.Map;

public record FrameComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public FrameComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.FRAME;
	}

	public String profileId() {
		return property("profile", "");
	}

	public static FrameComponent of(String id, String profileId) {
		return new FrameComponent(ComponentRef.of(id), ComponentProperties.of("profile", profileId));
	}
}
