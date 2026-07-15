package dev.aperture.core.component;

/**
 * Reference component combinations for common opening categories.
 * Types are compositions — not separate generator implementations.
 */
public final class ComponentAssemblyPresets {
	private ComponentAssemblyPresets() {
	}

	public static ComponentAssembly fixedWindow(String frameProfile, String glazingSystem) {
		return ComponentAssembly.of(
			FrameComponent.of("frame", frameProfile),
			GlassComponent.of("glazing", glazingSystem),
			MullionComponent.fromSource("mullions", "parameter:mullions")
		);
	}

	public static ComponentAssembly casementWindow(String frameProfile, String panelProfile, String glazingSystem) {
		return ComponentAssembly.of(
			FrameComponent.of("frame", frameProfile),
			PanelComponent.of("panel", panelProfile, "left"),
			GlassComponent.of("glazing", glazingSystem),
			MullionComponent.fromSource("mullions", "parameter:mullions")
		);
	}

	public static ComponentAssembly door(
		String frameProfile,
		String panelProfile,
		String glazingSystem,
		String hingeSide
	) {
		return ComponentAssembly.of(
			FrameComponent.of("door_frame", frameProfile),
			PanelComponent.of("door_leaf", panelProfile, hingeSide),
			GlassComponent.of("door_glass", glazingSystem),
			HardwareComponent.of("hinges", "hinge_set"),
			HandleComponent.of("handle"),
			SillComponent.of("threshold", frameProfile)
		);
	}

	public static ComponentAssembly curtainWall(String frameProfile, String glazingSystem) {
		return ComponentAssembly.of(
			FrameComponent.of("grid_frame", frameProfile),
			MullionComponent.fromSource("vertical_mullions", "parameter:cols"),
			MullionComponent.fromSource("horizontal_mullions", "parameter:rows"),
			GlassComponent.of("unit_glazing", glazingSystem),
			HeaderComponent.of("head", frameProfile),
			SillComponent.of("sill", frameProfile)
		);
	}
}
