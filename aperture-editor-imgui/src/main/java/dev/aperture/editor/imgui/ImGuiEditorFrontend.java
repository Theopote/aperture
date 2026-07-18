package dev.aperture.editor.imgui;

import java.util.Objects;

/**
 * Platform-neutral renderer seam for a future ImGui implementation.
 *
 * <p>Minecraft, GLFW, input and render-loop integration belong to
 * {@code aperture-fabric}; editor state and intents belong to
 * {@code aperture-editor}. No concrete ImGui widgets are implemented yet; they must eventually remain in this module.</p>
 */
public final class ImGuiEditorFrontend {
	private final ImGuiFrameRenderer frameRenderer;

	public ImGuiEditorFrontend(ImGuiFrameRenderer frameRenderer) {
		this.frameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
	}

	public void renderFrame() {
		frameRenderer.render();
	}
}
