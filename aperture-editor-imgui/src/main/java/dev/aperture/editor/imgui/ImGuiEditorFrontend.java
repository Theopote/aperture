package dev.aperture.editor.imgui;

import java.util.Objects;

/**
 * Platform-neutral owner of the ImGui editor frame lifecycle.
 *
 * <p>Minecraft, GLFW, input and render-loop integration belong to
 * {@code aperture-fabric}; editor state and intents belong to
 * {@code aperture-editor}. Concrete ImGui widgets remain in this module.</p>
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
