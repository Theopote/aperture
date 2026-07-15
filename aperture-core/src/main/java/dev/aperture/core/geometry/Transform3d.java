package dev.aperture.core.geometry;

/**
 * Placement transform for an opening instance.
 * Origin is the bottom-left-inside corner of the opening in world mm space.
 */
public record Transform3d(Vec3d origin, Facing facing) {
	public static Transform3d at(double x, double y, double z, Facing facing) {
		return new Transform3d(new Vec3d(x, y, z), facing);
	}

	/**
	 * Translates a local-space bounding box into world millimeter space.
	 */
	public BoundingBox applyTo(BoundingBox local) {
		return new BoundingBox(local.min().add(origin), local.max().add(origin));
	}
}
