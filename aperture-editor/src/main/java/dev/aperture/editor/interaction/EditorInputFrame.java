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
	boolean worldInputAllowed,
	WorldRay worldRay
) {
	public static EditorInputFrame idle() {
		return new EditorInputFrame(false, false, false, false, false, false, false, false, null);
	}

}
