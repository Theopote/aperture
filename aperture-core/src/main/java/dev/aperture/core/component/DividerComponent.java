package dev.aperture.core.component;

import java.util.Map;

public record DividerComponent(ComponentRef ref, Map<String, String> properties) implements OpeningComponent {
	public DividerComponent {
		properties = ComponentProperties.copyOf(properties);
	}

	@Override
	public ComponentKind kind() {
		return ComponentKind.DIVIDER;
	}

	public String source() {
		return property("source", "parameter:mullions");
	}

	/** @deprecated Use {@link MullionComponent#fromSource(String, String)}. */
	@Deprecated
	public static DividerComponent mullions(String id) {
		return fromSource(id, "parameter:mullions");
	}

	public static DividerComponent fromSource(String id, String source) {
		return new DividerComponent(ComponentRef.of(id), ComponentProperties.of("source", source));
	}
}
