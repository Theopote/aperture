package dev.aperture.editor.interaction;

/** Immutable, frontend-neutral input sampled once for an editor update. */
public record EditorInputFrame(
	boolean primaryPressed,
	boolean primaryDown,
	boolean primaryReleased,
	boolean cancelPressed,
	boolean shiftDown,
	boolean controlDown,
	boolean altDown,
	boolean uiCapturesMouse,
	boolean uiCapturesKeyboard,
	boolean pointerInsideWorldViewport,
	ScreenPoint cursor,
	WorldRay worldRay
) {
	public boolean worldInteractionAllowed() {
		return pointerInsideWorldViewport && !uiCapturesMouse && worldRay != null;
	}

	public static EditorInputFrame idle() {
		return new EditorInputFrame(false, false, false, false, false, false, false,
			false, false, false, null, null);
	}
}