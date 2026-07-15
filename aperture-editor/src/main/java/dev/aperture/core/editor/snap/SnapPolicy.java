package dev.aperture.core.editor.snap;

/**
 * Snap tolerance and grid settings for an editor session.
 */
public record SnapPolicy(
	boolean enabled,
	double toleranceMm,
	double gridStepMm
) {
	public static SnapPolicy defaults() {
		return new SnapPolicy(true, 10.0, 50.0);
	}
}
