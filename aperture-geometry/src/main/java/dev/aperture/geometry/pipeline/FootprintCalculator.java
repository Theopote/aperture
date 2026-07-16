package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;

/**
 * Calculates footprint (ground contact area) for openings.
 * Footprint is the 2D projection onto the XZ plane (horizontal plane).
 */
public final class FootprintCalculator {

    private FootprintCalculator() {
    }

    /**
     * Calculate footprint as 2D bounding box projected onto XZ plane.
     * Y coordinates are ignored, only X and Z extents are considered.
     */
    public static BoundingBox calculate(GeometryResult geometry) {
        if (geometry.solids().isEmpty()) {
            return BoundingBox.EMPTY;
        }

        // Find XZ extent of all geometry
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (var solid : geometry.solids()) {
            BoundingBox bounds = solid.shape().bounds();

            // Apply local transform if present
            if (solid.localTransform() != null && !solid.localTransform().isIdentity()) {
                bounds = transformBounds(bounds, solid.localTransform());
            }

            minX = Math.min(minX, bounds.min().x());
            minZ = Math.min(minZ, bounds.min().z());
            maxX = Math.max(maxX, bounds.max().x());
            maxZ = Math.max(maxZ, bounds.max().z());
        }

        if (Double.isInfinite(minX)) {
            return BoundingBox.EMPTY;
        }

        // Footprint spans full XZ extent but has minimal Y height (1mm)
        return new BoundingBox(
            new Vec3d(minX, 0, minZ),
            new Vec3d(maxX, 1, maxZ)  // 1mm height for visualization
        );
    }

    /**
     * Calculate footprint from collision bounds.
     * This is more efficient when collision is already computed.
     */
    public static BoundingBox fromCollision(BoundingBox collision) {
        if (collision == BoundingBox.EMPTY) {
            return BoundingBox.EMPTY;
        }

        return new BoundingBox(
            new Vec3d(collision.min().x(), 0, collision.min().z()),
            new Vec3d(collision.max().x(), 1, collision.max().z())
        );
    }

    /**
     * Calculate footprint with custom expansion (for clearance).
     */
    public static BoundingBox calculateWithExpansion(GeometryResult geometry, double expansion) {
        BoundingBox base = calculate(geometry);
        if (base == BoundingBox.EMPTY) {
            return base;
        }

        return new BoundingBox(
            new Vec3d(
                base.min().x() - expansion,
                0,
                base.min().z() - expansion
            ),
            new Vec3d(
                base.max().x() + expansion,
                1,
                base.max().z() + expansion
            )
        );
    }

    /**
     * Get footprint area in square millimeters.
     */
    public static double area(BoundingBox footprint) {
        if (footprint == BoundingBox.EMPTY) {
            return 0.0;
        }

        double width = footprint.max().x() - footprint.min().x();
        double depth = footprint.max().z() - footprint.min().z();
        return width * depth;
    }

    private static BoundingBox transformBounds(BoundingBox bounds, dev.aperture.math.Transform3d transform) {
        // Transform all 8 corners of the bounding box
        Vec3d[] corners = new Vec3d[]{
            bounds.min(),
            new Vec3d(bounds.max().x(), bounds.min().y(), bounds.min().z()),
            new Vec3d(bounds.min().x(), bounds.max().y(), bounds.min().z()),
            new Vec3d(bounds.min().x(), bounds.min().y(), bounds.max().z()),
            new Vec3d(bounds.max().x(), bounds.max().y(), bounds.min().z()),
            new Vec3d(bounds.max().x(), bounds.min().y(), bounds.max().z()),
            new Vec3d(bounds.min().x(), bounds.max().y(), bounds.max().z()),
            bounds.max()
        };

        Vec3d transformedMin = null;
        Vec3d transformedMax = null;

        for (Vec3d corner : corners) {
            Vec3d transformed = transform.origin().add(corner);

            if (transformedMin == null) {
                transformedMin = transformed;
                transformedMax = transformed;
            } else {
                transformedMin = new Vec3d(
                    Math.min(transformedMin.x(), transformed.x()),
                    Math.min(transformedMin.y(), transformed.y()),
                    Math.min(transformedMin.z(), transformed.z())
                );
                transformedMax = new Vec3d(
                    Math.max(transformedMax.x(), transformed.x()),
                    Math.max(transformedMax.y(), transformed.y()),
                    Math.max(transformedMax.z(), transformed.z())
                );
            }
        }

        return new BoundingBox(transformedMin, transformedMax);
    }
}
