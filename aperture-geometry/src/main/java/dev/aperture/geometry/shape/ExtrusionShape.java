package dev.aperture.geometry.shape;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec2d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.profile.ProfileCurve;

/**
 * Linear extrusion of a 2D profile between two 3D points.
 * <p>
 * Profile coordinates {@code (u, v)} map to {@code origin + u * profileU + v * profileV}.
 * The path runs from {@code pathStart} to {@code pathEnd}.
 */
public record ExtrusionShape(
	ProfileCurve profile,
	Vec3d pathStart,
	Vec3d pathEnd,
	Vec3d profileU,
	Vec3d profileV
) implements SolidShape {
	public ExtrusionShape {
		if (pathStart.equals(pathEnd)) {
			throw new IllegalArgumentException("pathStart and pathEnd must differ");
		}
	}

	public Vec3d pathVector() {
		return pathEnd.subtract(pathStart);
	}

	public Vec3d mapProfilePoint(Vec2d profilePoint) {
		return pathStart
			.add(profileU.scale(profilePoint.u()))
			.add(profileV.scale(profilePoint.v()));
	}

	@Override
	public BoundingBox bounds() {
		BoundingBox bounds = null;
		for (int i = 0; i < profile.segmentCount(); i++) {
			Vec2d a = profile.point(i);
			Vec2d b = profile.point(i + 1);
			Vec3d startA = mapProfilePoint(a);
			Vec3d startB = mapProfilePoint(b);
			Vec3d endA = startA.add(pathVector());
			Vec3d endB = startB.add(pathVector());
			BoundingBox segmentBounds = BoundingBox.ofPoints(startA, startB, endA, endB);
			bounds = bounds == null ? segmentBounds : bounds.union(segmentBounds);
		}
		return bounds;
	}
}
