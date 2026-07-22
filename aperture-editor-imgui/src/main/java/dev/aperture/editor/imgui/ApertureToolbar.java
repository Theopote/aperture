package dev.aperture.editor.imgui;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

final class ApertureToolbar {
	private final ApertureUiContext context;

	ApertureToolbar(ApertureUiContext context) { this.context = context; }

	void render() {
		if (ImGui.button("Select")) context.session.tools().cancelActiveTool();
		ImGui.sameLine(); disabled("Place");
		ImGui.sameLine(); disabled("Move");
		ImGui.sameLine(); disabled("Rotate");
		ImGui.sameLine(); disabled("Resize");
		ImGui.sameLine(); disabled("Attach");
		ImGui.sameLine(); disabled("Measure");
		ImGui.sameLine(); ImGui.separator();
		ImGui.sameLine();
		if (ImGui.checkbox("Snap", context.snap)) context.snap = !context.snap;
		ImGui.sameLine(); mode("Design", ApertureUiContext.Mode.DESIGN);
		ImGui.sameLine(); mode("Runtime", ApertureUiContext.Mode.RUNTIME);
		ImGui.sameLine(); disabled("Analyze");
		ImGui.separator();
	}

	private void mode(String label, ApertureUiContext.Mode mode) {
		boolean active = context.mode == mode;
		if (active) ImGui.pushStyleColor(ImGuiCol.Button,
			ApertureStyle.BLUE[0], ApertureStyle.BLUE[1], ApertureStyle.BLUE[2], 1);
		try {
			if (ImGui.button(label)) context.mode = mode;
		} finally {
			if (active) ImGui.popStyleColor();
		}
	}

	private static void disabled(String label) {
		ImGui.beginDisabled();
		try {
			ImGui.button(label);
		} finally {
			ImGui.endDisabled();
		}
		if (ImGui.isItemHovered()) ImGui.setTooltip("Tool transport is not connected in this build");
	}
}
