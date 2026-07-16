package dev.aperture.math;

/**
 * Placement transform for an opening instance or generated part.
 * Supports optional rotation about an arbitrary axis for panel kinematics.
 */
public record Transform3d(
	Vec3d origin,
	Facing facing,
	Vec3d rotationAxisOrigin,
	Vec3d rotationAxisDirection,
	double rotationRadians
) {
	public Transform3d(Vec3d origin, Facing facing) {
		this(origin, facing, Vec3d.ZERO, new Vec3d(0, 1, 0), 0.0);
	}

	public static Transform3d at(double x, double y, double z, Facing facing) {
		return new Transform3d(new Vec3d(x, y, z), facing);
	}

	public static Transform3d identity() {
		return at(0, 0, 0, Facing.NORTH);
	}

	public static Transform3d rotateAboutAxis(Vec3d axisOrigin, Vec3d axisDirection, double radians) {
		return new Transform3d(Vec3d.ZERO, Facing.NORTH, axisOrigin, axisDirection.normalize(), radians);
	}

	public boolean hasRotation() {
		return Math.abs(rotationRadians) > 1.0e-9;
	}

	public boolean isIdentity() {
		return this.equals(identity());
	}

	/**
	 * Translates a local-space bounding box into world millimeter space.
	 */
	public BoundingBox applyTo(BoundingBox local) {
		if (!hasRotation()) {
			return new BoundingBox(local.min().add(origin), local.max().add(origin));
		}
		return BoundingBox.ofPoints(
			transformPoint(local.min()),
			transformPoint(new Vec3d(local.max().x(), local.min().y(), local.min().z())),
			transformPoint(new Vec3d(local.min().x(), local.max().y(), local.min().z())),
			transformPoint(new Vec3d(local.max().x(), local.max().y(), local.min().z())),
			transformPoint(new Vec3d(local.min().x(), local.min().y(), local.max().z())),
			transformPoint(new Vec3d(local.max().x(), local.min().y(), local.max().z())),
			transformPoint(new Vec3d(local.min().x(), local.max().y(), local.max().z())),
			transformPoint(local.max())
		);
	}

	public Vec3d transformPoint(Vec3d localPoint) {
		Vec3d rotated = hasRotation()
			? rotateAroundAxis(localPoint, rotationAxisOrigin, rotationAxisDirection, rotationRadians)
			: localPoint;
		return rotated.add(origin);
	}

	public Vec3d transformDirection(Vec3d localDirection) {
		if (!hasRotation()) {
			return localDirection;
		}
		return rotateAroundAxis(localDirection, Vec3d.ZERO, rotationAxisDirection, rotationRadians);
	}

	private static Vec3d rotateAroundAxis(Vec3d point, Vec3d axisOrigin, Vec3d axisDirection, double radians) {
		Vec3d axis = axisDirection.normalize();
		Vec3d relative = point.subtract(axisOrigin);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		Vec3d cross = axis.cross(relative);
		double dot = axis.dot(relative);
		Vec3d rotated = relative.scale(cos)
			.add(cross.scale(sin))
			.add(axis.scale(dot * (1.0 - cos)));
		return rotated.add(axisOrigin);
	}
}
