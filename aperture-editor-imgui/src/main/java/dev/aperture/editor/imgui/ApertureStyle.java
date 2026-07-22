package dev.aperture.editor.imgui;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;

final class ApertureStyle {
	static final float STATUS_BAR_HEIGHT=24, LABEL_WIDTH=128;
	static final float[] BLUE={0.298f,0.604f,1f,1f}, SUCCESS={0.384f,0.761f,0.545f,1f};
	static final float[] WARNING={0.898f,0.706f,0.329f,1f}, ERROR={0.878f,0.424f,0.459f,1f}, RUNTIME={0.718f,0.549f,1f,1f};
	private ApertureStyle(){}
	static void push(){
		color(ImGuiCol.WindowBg,0x18,0x1B,0x20);color(ImGuiCol.ChildBg,0x20,0x24,0x2A);color(ImGuiCol.PopupBg,0x20,0x24,0x2A);
		color(ImGuiCol.Border,0x30,0x36,0x40);color(ImGuiCol.Header,0x27,0x3F,0x58);color(ImGuiCol.HeaderHovered,0x32,0x57,0x7B);
		color(ImGuiCol.Button,0x2A,0x30,0x38);color(ImGuiCol.ButtonHovered,0x35,0x43,0x53);ImGui.pushStyleColor(ImGuiCol.CheckMark,BLUE[0],BLUE[1],BLUE[2],1);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding,10,10);ImGui.pushStyleVar(ImGuiStyleVar.FramePadding,8,5);ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing,8,6);
	}
	static void pop(){ImGui.popStyleVar(3);ImGui.popStyleColor(9);}
	private static void color(int id,int r,int g,int b){ImGui.pushStyleColor(id,r/255f,g/255f,b/255f,1);}
	static void unavailable(String label){ImGui.beginDisabled();ImGui.menuItem(label);ImGui.endDisabled();if(ImGui.isItemHovered())ImGui.setTooltip("Not available in this build");}
}
