package dev.aperture.editor.imgui;

/** Renders one ImGui editor frame without owning the platform render loop. */
@FunctionalInterface
public interface ImGuiFrameRenderer {
	void render();
}
