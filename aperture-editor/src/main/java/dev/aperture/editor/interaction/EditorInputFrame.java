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

	/** Ray coordinates are expressed in host-world units; direction is normalized by the adapter. */
	public record WorldRay(double originX, double originY, double originZ,
		double directionX, double directionY, double directionZ) { }
}
