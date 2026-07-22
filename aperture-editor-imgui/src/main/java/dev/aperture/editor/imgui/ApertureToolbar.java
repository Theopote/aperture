package dev.aperture.editor.imgui;
import imgui.ImGui;
final class ApertureToolbar{
	private final ApertureUiContext c;ApertureToolbar(ApertureUiContext c){this.c=c;}
	void render(){if(ImGui.button("Select"))c.session.tools().cancelActiveTool();ImGui.sameLine();disabled("Place");ImGui.sameLine();disabled("Move");ImGui.sameLine();disabled("Rotate");ImGui.sameLine();disabled("Resize");ImGui.sameLine();disabled("Attach");ImGui.sameLine();disabled("Measure");ImGui.sameLine();ImGui.separator();ImGui.sameLine();if(ImGui.checkbox("Snap",c.snap))c.snap=!c.snap;ImGui.sameLine();mode("Design",ApertureUiContext.Mode.DESIGN);ImGui.sameLine();mode("Runtime",ApertureUiContext.Mode.RUNTIME);ImGui.sameLine();disabled("Analyze");ImGui.separator();}
	private void mode(String label,ApertureUiContext.Mode mode){if(c.mode==mode)ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button,ApertureStyle.BLUE[0],ApertureStyle.BLUE[1],ApertureStyle.BLUE[2],1);if(ImGui.button(label))c.mode=mode;if(c.mode==mode)ImGui.popStyleColor();}
	private static void disabled(String label){ImGui.beginDisabled();ImGui.button(label);ImGui.endDisabled();if(ImGui.isItemHovered())ImGui.setTooltip("Tool transport is not connected in this build");}
}
