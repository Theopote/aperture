package dev.aperture.core.instance;

/**
 * Operational state of a placed opening (e.g. door open ratio).
 */
public record OpeningState(double openRatio) {
	public static final OpeningState CLOSED = new OpeningState(0.0);

	public OpeningState {
		if (openRatio < 0.0 || openRatio > 1.0) {
			throw new IllegalArgumentException("openRatio must be in [0, 1]");
		}
	}
}
