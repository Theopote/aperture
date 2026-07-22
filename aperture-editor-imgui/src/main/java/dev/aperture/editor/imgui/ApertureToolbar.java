package dev.aperture.editor.imgui;

import dev.aperture.editor.model.session.ToolController;
import imgui.ImGui;
import imgui.flag.ImGuiCol;

final class ApertureToolbar {
	private final ApertureUiContext context;

	ApertureToolbar(ApertureUiContext context) { this.context = context; }

	void render() {
		tool("Select", ToolController.Tool.SELECT);
		ImGui.sameLine(); tool("Place", ToolController.Tool.PLACE);
		ImGui.sameLine(); tool("Move", ToolController.Tool.MOVE);
		ImGui.sameLine(); tool("Rotate", ToolController.Tool.ROTATE);
		ImGui.sameLine(); tool("Resize", ToolController.Tool.RESIZE);
		ImGui.sameLine(); tool("Attach", ToolController.Tool.ATTACH);
		ImGui.sameLine(); tool("Measure", ToolController.Tool.MEASURE);
		ImGui.sameLine(); ImGui.separator();
		ImGui.sameLine();
		if (ImGui.checkbox("Snap", context.snap)) context.snap = !context.snap;
		ImGui.sameLine(); mode("Design", ApertureUiContext.Mode.DESIGN);
		ImGui.sameLine(); mode("Runtime", ApertureUiContext.Mode.RUNTIME);
		ImGui.sameLine(); disabled("Analyze", "Analysis mode is not available yet");
		ImGui.separator();
	}

	private void tool(String label, ToolController.Tool tool) {
		var tools = context.session.tools();
		boolean active = tools.activeTool() == tool;
		boolean available = tools.available(tool);
		if (active) ImGui.pushStyleColor(ImGuiCol.Button,
			ApertureStyle.BLUE[0], ApertureStyle.BLUE[1], ApertureStyle.BLUE[2], 1);
		if (!available) ImGui.beginDisabled();
		try {
			if (ImGui.button(label + "##Tool")) tools.activate(tool);
		} finally {
			if (!available) ImGui.endDisabled();
			if (active) ImGui.popStyleColor();
		}
		if (ImGui.isItemHovered()) {
			String tooltip = available ? toolHint(tool) : tools.disabledReason(tool);
			if (!tooltip.isBlank()) ImGui.setTooltip(tooltip);
		}
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

	private static String toolHint(ToolController.Tool tool) {
		return switch (tool) {
			case SELECT -> "Select architectural objects";
			case PLACE -> "Enter the Minecraft world placement workflow";
			case ROTATE -> "Use the world-space rotation gizmo on the current preview";
			case RESIZE -> "Drag world-space resize handles on the current preview";
			default -> "";
		};
	}

	private static void disabled(String label, String reason) {
		ImGui.beginDisabled();
		try { ImGui.button(label); } finally { ImGui.endDisabled(); }
		if (ImGui.isItemHovered()) ImGui.setTooltip(reason);
	}
}
