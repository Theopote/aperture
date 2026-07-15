package dev.aperture.core.component;

import java.util.Map;

/**
 * One logical part of an opening assembly (frame rail set, operable panel, glazing unit, etc.).
 */
public sealed interface OpeningComponent permits
	FrameComponent,
	PanelComponent,
	GlassComponent,
	HardwareComponent,
	TrimComponent,
	SillComponent,
	HeaderComponent,
	DividerComponent,
	DecorationComponent {

	ComponentRef ref();

	ComponentKind kind();

	Map<String, String> properties();

	default String property(String key) {
		return properties().get(key);
	}

	default String property(String key, String defaultValue) {
		return properties().getOrDefault(key, defaultValue);
	}
}
