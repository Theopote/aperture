package dev.aperture.geometry.profile;

import dev.aperture.math.Vec2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Scales catalog profiles to match runtime frame parameters.
 */
public final class ProfileScaler {
	private ProfileScaler() {
	}

	public static ProfileCurve scaleUniform(ProfileCurve source, double scale) {
		if (Math.abs(scale - 1.0) < 1.0e-9) {
			return source;
		}
		ProfileBounds bounds = source.bounds();
		List<Vec2d> scaled = new ArrayList<>();
		for (Vec2d point : source.points()) {
			scaled.add(new Vec2d(
				bounds.minU() + (point.u() - bounds.minU()) * scale,
				bounds.minV() + (point.v() - bounds.minV()) * scale
			));
		}
		return ProfileCurve.fromPoints(scaled);
	}

	public static ProfileCurve scaleToWidth(ProfileCurve source, double targetWidth) {
		double scale = targetWidth / source.bounds().width();
		return scaleUniform(source, scale);
	}

	public static ProfileCurve scaleToSize(ProfileCurve source, double targetWidth, double targetDepth) {
		ProfileBounds bounds = source.bounds();
		List<Vec2d> scaled = new ArrayList<>();
		double scaleU = targetWidth / bounds.width();
		double scaleV = targetDepth / bounds.depth();
		for (Vec2d point : source.points()) {
			scaled.add(new Vec2d(
				bounds.minU() + (point.u() - bounds.minU()) * scaleU,
				bounds.minV() + (point.v() - bounds.minV()) * scaleV
			));
		}
		return ProfileCurve.fromPoints(scaled);
	}

	public static ProfileDefinition scaleToFrameWidth(ProfileDefinition source, double frameWidth) {
		return new ProfileDefinition(
			source.id(),
			source.name(),
			scaleToWidth(source.curve(), frameWidth)
		);
	}

	public static ProfileDefinition scaleToFrameSize(ProfileDefinition source, double frameWidth, double frameDepth) {
		return new ProfileDefinition(
			source.id(),
			source.name(),
			scaleToSize(source.curve(), frameWidth, frameDepth)
		);
	}
}
