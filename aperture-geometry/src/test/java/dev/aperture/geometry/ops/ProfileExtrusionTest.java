package dev.aperture.geometry.ops;

import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.ExtrusionShape;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec2d;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for profile extrusion functionality.
 */
class ProfileExtrusionTest {

    @Test
    void extrudeLinear_rectangularProfile_generatesCorrectMesh() {
        // Given: A rectangular profile (50mm x 80mm)
        ProfileCurve profile = ProfileCurve.rectangle(0, 0, 50, 80);

        Vec3d pathStart = new Vec3d(0, 0, 0);
        Vec3d pathEnd = new Vec3d(1000, 0, 0);  // Extrude 1000mm along X
        Vec3d profileU = new Vec3d(0, 1, 0);     // Profile U maps to Y
        Vec3d profileV = new Vec3d(0, 0, 1);     // Profile V maps to Z

        // When: Create extrusion shape and generate mesh
        ExtrusionShape extrusion = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
        Mesh mesh = ShapeMesher.meshLocal(extrusion);

        // Then: Mesh should have vertices and faces
        assertNotNull(mesh);
        assertTrue(mesh.vertexCount() > 0, "Mesh should have vertices");
        assertTrue(mesh.faceCount() > 0, "Mesh should have faces");

        // Verify bounds
        var bounds = extrusion.bounds();
        assertEquals(0, bounds.min().x(), 0.001);
        assertEquals(0, bounds.min().y(), 0.001);
        assertEquals(0, bounds.min().z(), 0.001);
        assertEquals(1000, bounds.max().x(), 0.001);
        assertEquals(50, bounds.max().y(), 0.001);
        assertEquals(80, bounds.max().z(), 0.001);
    }

    @Test
    void extrudeLinear_lProfile_generatesCorrectMesh() {
        // Given: An L-shaped profile (typical frame profile)
        List<Vec2d> lProfilePoints = List.of(
            new Vec2d(0, 0),
            new Vec2d(50, 0),
            new Vec2d(50, 10),
            new Vec2d(10, 10),
            new Vec2d(10, 80),
            new Vec2d(0, 80)
        );
        ProfileCurve profile = ProfileCurve.fromPoints(lProfilePoints);

        Vec3d pathStart = new Vec3d(0, 0, 0);
        Vec3d pathEnd = new Vec3d(0, 1200, 0);  // Extrude 1200mm along Y
        Vec3d profileU = new Vec3d(1, 0, 0);     // Profile U maps to X
        Vec3d profileV = new Vec3d(0, 0, 1);     // Profile V maps to Z

        // When: Create extrusion and generate mesh
        ExtrusionShape extrusion = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
        Mesh mesh = ShapeMesher.meshLocal(extrusion);

        // Then: Mesh generated successfully
        assertNotNull(mesh);
        assertTrue(mesh.vertexCount() > 0);

        // Should have 6 segments for the L-profile
        assertEquals(6, profile.segmentCount());

        // Should have side faces + 2 caps
        // 6 segments × 2 triangles per quad = 12 triangles for sides
        // 2 caps with (6-2) = 4 triangles each = 8 triangles
        // Total: 20 triangles minimum
        assertTrue(mesh.faceCount() >= 20,
            "Expected at least 20 faces (12 sides + 8 caps), got " + mesh.faceCount());
    }

    @Test
    void extrudeLinear_withTransform_appliesTransformCorrectly() {
        // Given: A simple rectangular profile
        ProfileCurve profile = ProfileCurve.rectangle(0, 0, 100, 100);

        Vec3d pathStart = Vec3d.ZERO;
        Vec3d pathEnd = new Vec3d(500, 0, 0);
        Vec3d profileU = new Vec3d(0, 1, 0);
        Vec3d profileV = new Vec3d(0, 0, 1);

        ExtrusionShape extrusion = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);

        // Transform: translate by (1000, 2000, 3000)
        Transform3d transform = Transform3d.at(1000, 2000, 3000, dev.aperture.math.Facing.NORTH);

        // When: Generate mesh with transform
        Mesh mesh = ShapeMesher.mesh(extrusion, transform);

        // Then: All vertices should be translated
        assertNotNull(mesh);
        assertTrue(mesh.vertexCount() > 0);

        // Check that at least one vertex has been translated
        boolean hasTranslatedVertex = false;
        for (int i = 0; i < mesh.vertexCount(); i++) {
            var vertex = mesh.vertex(i);
            if (vertex.x() >= 1000 || vertex.y() >= 2000 || vertex.z() >= 3000) {
                hasTranslatedVertex = true;
                break;
            }
        }
        assertTrue(hasTranslatedVertex, "Mesh should have translated vertices");
    }

    @Test
    void extrudeLinear_verticalExtrusion_generatesCorrectBounds() {
        // Given: Profile extruded vertically (typical for window/door frame)
        ProfileCurve profile = ProfileCurve.rectangle(0, 0, 50, 60);

        Vec3d pathStart = new Vec3d(100, 0, 200);
        Vec3d pathEnd = new Vec3d(100, 1500, 200);  // Vertical extrusion
        Vec3d profileU = new Vec3d(1, 0, 0);
        Vec3d profileV = new Vec3d(0, 0, 1);

        // When: Create extrusion
        ExtrusionShape extrusion = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);

        // Then: Bounds should span the vertical distance
        var bounds = extrusion.bounds();
        assertEquals(100, bounds.min().x(), 0.001);
        assertEquals(0, bounds.min().y(), 0.001);
        assertEquals(200, bounds.min().z(), 0.001);

        assertEquals(150, bounds.max().x(), 0.001);  // 100 + 50
        assertEquals(1500, bounds.max().y(), 0.001);
        assertEquals(260, bounds.max().z(), 0.001);  // 200 + 60
    }

    @Test
    void extrudeLinear_zeroLengthPath_throwsException() {
        // Given: Profile with same start and end point
        ProfileCurve profile = ProfileCurve.rectangle(0, 0, 50, 50);

        Vec3d pathStart = new Vec3d(100, 100, 100);
        Vec3d pathEnd = new Vec3d(100, 100, 100);  // Same as start
        Vec3d profileU = new Vec3d(1, 0, 0);
        Vec3d profileV = new Vec3d(0, 0, 1);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () ->
            ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV)
        );
    }

    @Test
    void profileCurve_minimumPoints_requiresAtLeastThree() {
        // Given: Less than 3 points
        List<Vec2d> twoPoints = List.of(
            new Vec2d(0, 0),
            new Vec2d(100, 0)
        );

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () ->
            ProfileCurve.fromPoints(twoPoints)
        );
    }

    @Test
    void profileCurve_bounds_calculatesCorrectly() {
        // Given: L-shaped profile
        List<Vec2d> points = List.of(
            new Vec2d(0, 0),
            new Vec2d(50, 0),
            new Vec2d(50, 10),
            new Vec2d(10, 10),
            new Vec2d(10, 80),
            new Vec2d(0, 80)
        );
        ProfileCurve profile = ProfileCurve.fromPoints(points);

        // When: Get bounds
        var bounds = profile.bounds();

        // Then: Bounds should encompass all points
        assertEquals(0, bounds.minU(), 0.001);
        assertEquals(0, bounds.minV(), 0.001);
        assertEquals(50, bounds.maxU(), 0.001);
        assertEquals(80, bounds.maxV(), 0.001);
    }
}
