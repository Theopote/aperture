package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for collision and footprint calculation.
 */
class CollisionFootprintTest {

    @Test
    void collisionCalculator_emptyGeometry_returnsEmpty() {
        GeometryResult empty = new GeometryResult(List.of());
        BoundingBox collision = CollisionCalculator.calculate(empty);
        assertEquals(BoundingBox.EMPTY, collision);
    }

    @Test
    void collisionCalculator_singleSolid_returnsCorrectBounds() {
        // Given: A single box solid
        var bounds = new BoundingBox(new Vec3d(0, 0, 0), new Vec3d(100, 200, 50));
        var solid = createMockSolid(bounds);
        var geometry = new GeometryResult(List.of(solid));

        // When: Calculate collision
        BoundingBox collision = CollisionCalculator.calculate(geometry);

        // Then: Should match solid bounds
        assertEquals(0, collision.min().x(), 0.001);
        assertEquals(0, collision.min().y(), 0.001);
        assertEquals(0, collision.min().z(), 0.001);
        assertEquals(100, collision.max().x(), 0.001);
        assertEquals(200, collision.max().y(), 0.001);
        assertEquals(50, collision.max().z(), 0.001);
    }

    @Test
    void collisionCalculator_multipleSolids_unionsAllBounds() {
        // Given: Two solids at different positions
        var bounds1 = new BoundingBox(new Vec3d(0, 0, 0), new Vec3d(100, 100, 100));
        var bounds2 = new BoundingBox(new Vec3d(50, 50, 50), new Vec3d(200, 200, 200));
        var solid1 = createMockSolid(bounds1);
        var solid2 = createMockSolid(bounds2);
        var geometry = new GeometryResult(List.of(solid1, solid2));

        // When: Calculate collision
        BoundingBox collision = CollisionCalculator.calculate(geometry);

        // Then: Should encompass both solids
        assertEquals(0, collision.min().x(), 0.001);
        assertEquals(0, collision.min().y(), 0.001);
        assertEquals(0, collision.min().z(), 0.001);
        assertEquals(200, collision.max().x(), 0.001);
        assertEquals(200, collision.max().y(), 0.001);
        assertEquals(200, collision.max().z(), 0.001);
    }

    @Test
    void collisionCalculator_withExpansion_expandsBounds() {
        // Given: Solid with known bounds
        var bounds = new BoundingBox(new Vec3d(100, 100, 100), new Vec3d(200, 200, 200));
        var solid = createMockSolid(bounds);
        var geometry = new GeometryResult(List.of(solid));

        // When: Calculate with 10mm expansion
        BoundingBox collision = CollisionCalculator.calculateWithExpansion(geometry, 10.0);

        // Then: Should be expanded on all sides
        assertEquals(90, collision.min().x(), 0.001);
        assertEquals(90, collision.min().y(), 0.001);
        assertEquals(90, collision.min().z(), 0.001);
        assertEquals(210, collision.max().x(), 0.001);
        assertEquals(210, collision.max().y(), 0.001);
        assertEquals(210, collision.max().z(), 0.001);
    }

    @Test
    void footprintCalculator_emptyGeometry_returnsEmpty() {
        GeometryResult empty = new GeometryResult(List.of());
        BoundingBox footprint = FootprintCalculator.calculate(empty);
        assertEquals(BoundingBox.EMPTY, footprint);
    }

    @Test
    void footprintCalculator_singleSolid_projectsToXZ() {
        // Given: A solid spanning XYZ space
        var bounds = new BoundingBox(new Vec3d(10, 50, 20), new Vec3d(100, 200, 80));
        var solid = createMockSolid(bounds);
        var geometry = new GeometryResult(List.of(solid));

        // When: Calculate footprint
        BoundingBox footprint = FootprintCalculator.calculate(geometry);

        // Then: Should project onto XZ plane (Y ignored)
        assertEquals(10, footprint.min().x(), 0.001);
        assertEquals(0, footprint.min().y(), 0.001);  // Always 0
        assertEquals(20, footprint.min().z(), 0.001);
        assertEquals(100, footprint.max().x(), 0.001);
        assertEquals(1, footprint.max().y(), 0.001);  // Minimal height
        assertEquals(80, footprint.max().z(), 0.001);
    }

    @Test
    void footprintCalculator_fromCollision_projectsCorrectly() {
        // Given: Collision bounds
        var collision = new BoundingBox(new Vec3d(50, 100, 30), new Vec3d(150, 250, 90));

        // When: Calculate footprint from collision
        BoundingBox footprint = FootprintCalculator.fromCollision(collision);

        // Then: X and Z should match, Y should be reset
        assertEquals(50, footprint.min().x(), 0.001);
        assertEquals(0, footprint.min().y(), 0.001);
        assertEquals(30, footprint.min().z(), 0.001);
        assertEquals(150, footprint.max().x(), 0.001);
        assertEquals(1, footprint.max().y(), 0.001);
        assertEquals(90, footprint.max().z(), 0.001);
    }

    @Test
    void footprintCalculator_area_calculatesCorrectly() {
        // Given: Footprint with known dimensions
        var footprint = new BoundingBox(
            new Vec3d(0, 0, 0),
            new Vec3d(1000, 1, 500)  // 1000mm × 500mm
        );

        // When: Calculate area
        double area = FootprintCalculator.area(footprint);

        // Then: Should be width × depth
        assertEquals(500000.0, area, 0.001);  // 1000 × 500 = 500,000 mm²
    }

    @Test
    void footprintCalculator_withExpansion_expandsXZOnly() {
        // Given: Solid
        var bounds = new BoundingBox(new Vec3d(100, 50, 200), new Vec3d(300, 150, 400));
        var solid = createMockSolid(bounds);
        var geometry = new GeometryResult(List.of(solid));

        // When: Calculate with 20mm expansion
        BoundingBox footprint = FootprintCalculator.calculateWithExpansion(geometry, 20.0);

        // Then: X and Z expanded, Y reset
        assertEquals(80, footprint.min().x(), 0.001);
        assertEquals(0, footprint.min().y(), 0.001);
        assertEquals(180, footprint.min().z(), 0.001);
        assertEquals(320, footprint.max().x(), 0.001);
        assertEquals(1, footprint.max().y(), 0.001);
        assertEquals(420, footprint.max().z(), 0.001);
    }

    @Test
    void footprintCalculator_emptyFootprint_zeroArea() {
        double area = FootprintCalculator.area(BoundingBox.EMPTY);
        assertEquals(0.0, area, 0.001);
    }

    // Helper to create mock solid with specific bounds
    private GeometrySolid createMockSolid(BoundingBox bounds) {
        return GeometrySolid.of(
            "test",
            "test",
            dev.aperture.geometry.model.GeometryLayer.OPAQUE,
            new dev.aperture.geometry.shape.BoxShape(bounds)
        );
    }
}
