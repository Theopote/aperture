package dev.aperture.geometry.profile;

import dev.aperture.math.Vec2d;

/**
 * Axis-aligned bounds of a profile curve in local (u, v) space.
 */
public record ProfileBounds(double minU, double minV, double maxU, double maxV) {
	public double width() {
		return maxU - minU;
	}

	public double depth() {
		return maxV - minV;
	}
}
