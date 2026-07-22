package dev.aperture.editor.imgui;

import imgui.type.ImInt;
import imgui.flag.ImGuiDir;
import imgui.internal.flag.ImGuiDockNodeFlags;

/** Builds the versioned five-region product workspace. */
final class DearImGuiLayout {
	private static final float NAVIGATOR_RATIO = 0.20f;
	private static final float CONTEXT_RATIO = 0.26f;
	private static final float CONSOLE_RATIO = 0.28f;
	private static final float RUNTIME_RATIO = 0.38f;

	private DearImGuiLayout() {}

	static void buildDefault(int root, float width, float height) {
		imgui.internal.ImGui.dockBuilderRemoveNode(root);
		imgui.internal.ImGui.dockBuilderAddNode(root, ImGuiDockNodeFlags.DockSpace);
		imgui.internal.ImGui.dockBuilderSetNodeSize(root, width, height);

		ImInt center = new ImInt(root);
		ImInt navigator = new ImInt();
		ImInt context = new ImInt();
		ImInt console = new ImInt();
		ImInt runtime = new ImInt();
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Left, NAVIGATOR_RATIO, navigator, center);
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Right, CONTEXT_RATIO, context, center);
		imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Down, CONSOLE_RATIO, console, center);
		imgui.internal.ImGui.dockBuilderSplitNode(context.get(), ImGuiDir.Down, RUNTIME_RATIO, runtime, context);

		imgui.internal.ImGui.dockBuilderDockWindow("Project Navigator", navigator.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Inspector", context.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Context Runtime", runtime.get());
		imgui.internal.ImGui.dockBuilderDockWindow("Activity Console", console.get());
		imgui.internal.ImGui.dockBuilderFinish(root);
	}
}
