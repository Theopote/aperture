package dev.aperture.editor.imgui;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.flag.ImGuiDir;
import imgui.internal.flag.ImGuiDockNodeFlags;

/** Builds the first-run professional editor layout. */
final class DearImGuiLayout {
	private DearImGuiLayout() {}
	static void buildDefault(int root) {
		imgui.internal.ImGui.dockBuilderRemoveNode(root);
		imgui.internal.ImGui.dockBuilderAddNode(root, ImGuiDockNodeFlags.DockSpace);
		var viewport = ImGui.getMainViewport();
		imgui.internal.ImGui.dockBuilderSetNodeSize(root, viewport.getWorkSizeX(), viewport.getWorkSizeY());
		ImInt center = new ImInt(root);
		ImInt left = new ImInt(); ImInt right = new ImInt(); ImInt bottom = new ImInt();
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Left, 0.20f, left, center);
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Right, 0.26f, right, center);
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Down, 0.28f, bottom, center);
		imgui.internal.ImGui.dockBuilderDockWindow("Project Navigator", left.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Inspector", right.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Context Runtime", right.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Activity Console", bottom.get());
		imgui.internal.ImGui.dockBuilderFinish(root);
	}
}
