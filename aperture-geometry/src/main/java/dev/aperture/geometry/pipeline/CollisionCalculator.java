package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;

/**
 * Calculates collision bounds for openings.
 * Collision bounds determine physical interaction volume in the world.
 */
public final class CollisionCalculator {

    private CollisionCalculator() {
    }

    /**
     * Calculate tight collision bounds from geometry result.
     * Returns axis-aligned bounding box encompassing all geometry.
     */
    public static BoundingBox calculate(GeometryResult geometry) {
        if (geometry.solids().isEmpty()) {
            return BoundingBox.EMPTY;
        }

        // Union all solid bounds
        BoundingBox collision = null;
        for (var solid : geometry.solids()) {
            BoundingBox solidBounds = solid.shape().bounds();

            // Apply local transform if present
            if (solid.localTransform() != null && !solid.localTransform().isIdentity()) {
                solidBounds = transformBounds(solidBounds, solid.localTransform());
            }

            collision = collision == null ? solidBounds : collision.union(solidBounds);
        }

        return collision != null ? collision : BoundingBox.EMPTY;
    }

    /**
     * Calculate collision bounds with custom expansion.
     * Useful for adding tolerance or clearance around geometry.
     */
    public static BoundingBox calculateWithExpansion(GeometryResult geometry, double expansion) {
        BoundingBox base = calculate(geometry);
        if (base == BoundingBox.EMPTY) {
            return base;
        }
        return new BoundingBox(
            new Vec3d(
                base.min().x() - expansion,
                base.min().y() - expansion,
                base.min().z() - expansion
            ),
            new Vec3d(
                base.max().x() + expansion,
                base.max().y() + expansion,
                base.max().z() + expansion
            )
        );
    }

    /**
     * Calculate collision bounds from existing bounds (already computed).
     * This is the most efficient method when geometry bounds are already available.
     */
    public static BoundingBox fromBounds(BoundingBox geometryBounds) {
        return geometryBounds;
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
