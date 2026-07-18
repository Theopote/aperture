package dev.aperture.editor.imgui.platform;

/** Captures and restores the host render state around ImGui draw submission. */
public interface RenderStateGuard {
	Snapshot capture();
	void restore(Snapshot snapshot);
	interface Snapshot {}
}
