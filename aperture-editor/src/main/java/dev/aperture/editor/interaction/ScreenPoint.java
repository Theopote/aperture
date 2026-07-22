package dev.aperture.editor.interaction;

/** Physical window-pixel coordinate with the origin at the top-left. */
public record ScreenPoint(double x, double y) {
	public double distanceSquared(ScreenPoint other) {
		double dx = x - other.x;
		double dy = y - other.y;
		return dx * dx + dy * dy;
	}
}
