package dev.aperture.client.editor;

/** Last ImGui input policy published by the render-side editor runtime. */
public final class EditorUiCaptureState {
	private static volatile Snapshot current = Snapshot.INACTIVE;

	private EditorUiCaptureState() { }

	public static Snapshot current() { return current; }

	public static void publish(boolean capturesMouse, boolean capturesKeyboard,
		boolean capturesText, boolean pointerInsideWorldViewport) {
		current = new Snapshot(true, capturesMouse, capturesKeyboard || capturesText, pointerInsideWorldViewport);
	}

	public static void clear() { current = Snapshot.INACTIVE; }

	public record Snapshot(boolean editorActive, boolean capturesMouse,
		boolean capturesKeyboard, boolean pointerInsideWorldViewport) {
		private static final Snapshot INACTIVE = new Snapshot(false, false, false, true);
	}
}
