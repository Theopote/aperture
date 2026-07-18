package dev.aperture.editor.imgui.workspace;

import java.util.Map;

public record WorkspaceLayout(int version, Map<String, Boolean> visibleWindows, float uiScale) {
	public static final int CURRENT_VERSION = 1;
	public WorkspaceLayout {
		if (version < 1) throw new IllegalArgumentException("version must be positive");
		visibleWindows = Map.copyOf(visibleWindows);
		if (!Float.isFinite(uiScale) || uiScale <= 0) throw new IllegalArgumentException("uiScale must be positive");
	}
	public static WorkspaceLayout defaults() {
		return new WorkspaceLayout(CURRENT_VERSION, Map.of("outliner",true,"inspector",true,"runtime",true,"history",true,"diagnostics",true,"viewport",true), 1f);
	}
}
