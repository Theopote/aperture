package dev.aperture.editor.imgui.platform;

import dev.aperture.editor.imgui.input.EditorInputPolicy;

/** Native/backend seam implemented by the Minecraft client source set. */
public interface ImGuiBackend extends AutoCloseable {
	void initialize();
	void beginFrame(float framebufferScale);
	void renderDrawData();
	EditorInputPolicy inputPolicy();
	@Override void close();
}
