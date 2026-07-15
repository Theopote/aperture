package dev.aperture.core.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Ordered set of components that compose one opening type.
 * Window, door, and curtain wall differ only by which components are present.
 */
public final class ComponentAssembly {
	private final List<OpeningComponent> components;

	private ComponentAssembly(List<OpeningComponent> components) {
		this.components = List.copyOf(components);
	}

	public static ComponentAssembly empty() {
		return new ComponentAssembly(List.of());
	}

	public static ComponentAssembly of(OpeningComponent... components) {
		return new ComponentAssembly(List.of(components));
	}

	public static ComponentAssembly of(List<OpeningComponent> components) {
		return new ComponentAssembly(components);
	}

	public List<OpeningComponent> all() {
		return components;
	}

	public int size() {
		return components.size();
	}

	public boolean isEmpty() {
		return components.isEmpty();
	}

	public boolean has(ComponentKind kind) {
		return components.stream().anyMatch(component -> component.kind() == kind);
	}

	public Optional<OpeningComponent> findById(String id) {
		return components.stream()
			.filter(component -> component.ref().id().equals(id))
			.findFirst();
	}

	public List<OpeningComponent> ofKind(ComponentKind kind) {
		return components.stream()
			.filter(component -> component.kind() == kind)
			.toList();
	}

	public Optional<FrameComponent> frame() {
		return firstOfKind(ComponentKind.FRAME, FrameComponent.class);
	}

	public Optional<PanelComponent> panel() {
		return firstOfKind(ComponentKind.PANEL, PanelComponent.class);
	}

	public Optional<GlassComponent> glass() {
		return firstOfKind(ComponentKind.GLASS, GlassComponent.class);
	}

	public Optional<DividerComponent> divider() {
		return firstOfKind(ComponentKind.DIVIDER, DividerComponent.class);
	}

	public Optional<HeaderComponent> header() {
		return firstOfKind(ComponentKind.HEADER, HeaderComponent.class);
	}

	public Optional<SillComponent> sill() {
		return firstOfKind(ComponentKind.SILL, SillComponent.class);
	}

	public Optional<HardwareComponent> hardware() {
		return firstOfKind(ComponentKind.HARDWARE, HardwareComponent.class);
	}

	public boolean hasLegacyKey(String legacyKey) {
		return switch (legacyKey) {
			case "frame" -> has(ComponentKind.FRAME);
			case "panel" -> has(ComponentKind.PANEL);
			case "glazing" -> has(ComponentKind.GLASS);
			default -> findById(legacyKey).isPresent();
		};
	}

	/**
	 * Converts the pre-component-system JSON map into a typed assembly.
	 */
	@SuppressWarnings("unchecked")
	public static ComponentAssembly fromLegacyMap(Map<String, Object> legacy) {
		if (legacy == null || legacy.isEmpty()) {
			return empty();
		}
		List<OpeningComponent> components = new ArrayList<>();
		for (Map.Entry<String, Object> entry : legacy.entrySet()) {
			String key = entry.getKey();
			Map<String, String> props = toStringMap(entry.getValue());
			components.add(switch (key) {
				case "frame" -> FrameComponent.of(key, props.getOrDefault("profile", ""));
				case "panel" -> PanelComponent.of(
					key,
					props.getOrDefault("profile", ""),
					props.getOrDefault("hinge", "left")
				);
				case "glazing" -> GlassComponent.of(key, props.getOrDefault("system", ""));
				default -> parseByKind(key, props);
			});
		}
		return new ComponentAssembly(components);
	}

	private static OpeningComponent parseByKind(String id, Map<String, String> props) {
		String kindKey = props.getOrDefault("kind", id);
		return switch (ComponentKind.fromJsonKey(kindKey)) {
			case FRAME -> FrameComponent.of(id, props.getOrDefault("profile", ""));
			case PANEL -> PanelComponent.of(
				id,
				props.getOrDefault("profile", ""),
				props.getOrDefault("hinge", "left")
			);
			case GLASS -> GlassComponent.of(id, props.getOrDefault("system", ""));
			case HARDWARE -> HardwareComponent.of(id, props.getOrDefault("type", "generic"));
			case TRIM -> TrimComponent.of(id, props.getOrDefault("profile", ""));
			case SILL -> SillComponent.of(id, props.getOrDefault("profile", ""));
			case HEADER -> HeaderComponent.of(id, props.getOrDefault("profile", ""));
			case DIVIDER -> new DividerComponent(ComponentRef.of(id), props);
			case DECORATION -> DecorationComponent.of(id, props.getOrDefault("style", "default"));
		};
	}

	private static Map<String, String> toStringMap(Object raw) {
		if (!(raw instanceof Map<?, ?> map)) {
			return Map.of();
		}
		Map<String, String> props = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				props.put(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		return props;
	}

	private <T extends OpeningComponent> Optional<T> firstOfKind(ComponentKind kind, Class<T> type) {
		return components.stream()
			.filter(component -> component.kind() == kind)
			.map(type::cast)
			.findFirst();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ComponentAssembly assembly && components.equals(assembly.components);
	}

	@Override
	public int hashCode() {
		return Objects.hash(components);
	}

	@Override
	public String toString() {
		return "ComponentAssembly" + components;
	}
}
