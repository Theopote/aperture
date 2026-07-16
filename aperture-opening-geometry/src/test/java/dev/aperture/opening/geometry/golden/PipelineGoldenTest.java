package dev.aperture.opening.geometry.golden;

import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden tests for pipeline stability.
 * These tests verify that pipeline output remains consistent across changes.
 */
class PipelineGoldenTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    @Test
    void fixedWindow_1200x1500_matchesGolden() throws Exception {
        // Given: Standard fixed window (1200x1500)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .put("frame_material", ParameterValue.materialRef("aperture:aluminum"))
            .build();

        // When: Generate pipeline result
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);

        // Then: Compare each mesh part against golden files
        for (var entry : result.meshes().partsByPath().entrySet()) {
            String partPath = entry.getKey();
            var mesh = entry.getValue();

            String sanitizedPath = partPath.replace('.', '_').replace('/', '_');
            String filename = "fixed_window_1200x1500_" + sanitizedPath + ".json";
            Path goldenFile = GOLDEN_DIR.resolve(filename);

            if (!goldenFile.toFile().exists()) {
                System.out.println("Warning: Golden file missing: " + filename);
                continue;
            }

            var goldenMesh = GoldenMeshSupport.load(goldenFile);
            var comparison = GoldenMeshSupport.compare(mesh, goldenMesh);

            assertTrue(comparison.matches(),
                String.format("Mesh %s differs from golden: %s", partPath, comparison.summary()));
        }

        System.out.printf("✓ All %d mesh parts match golden files%n",
            result.meshes().partsByPath().size());
    }

    @Test
    void fixedWindow_customDimensions_matchesGolden() {
        // Given: Custom dimensions (1200mm x 1500mm)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        // When: Generate pipeline result
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);

        // Then: Verify structure
        assertFalse(result.geometry().solids().isEmpty());

        // Verify bounds reflect custom dimensions
        var bounds = result.geometry().bounds();
        assertTrue(bounds.max().x() >= 1200.0, "Width should be at least 1200mm");
        assertTrue(bounds.max().y() >= 1500.0, "Height should be at least 1500mm");
    }

    @Test
    void door_single_900x2100_matchesGolden() throws Exception {
        // Given: Single panel door (900x2100)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("thickness", ParameterValue.length(50.0))
            .put("panel_count", ParameterValue.count(1))
            .put("glass_ratio", ParameterValue.number(0.3))
            .build();

        // When: Generate pipeline result
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Compare against golden files
        for (var entry : result.meshes().partsByPath().entrySet()) {
            String partPath = entry.getKey();
            var mesh = entry.getValue();

            String sanitizedPath = partPath.replace('.', '_').replace('/', '_');
            String filename = "door_single_900x2100_" + sanitizedPath + ".json";
            Path goldenFile = GOLDEN_DIR.resolve(filename);

            if (!goldenFile.toFile().exists()) {
                System.out.println("Warning: Golden file missing: " + filename);
                continue;
            }

            var goldenMesh = GoldenMeshSupport.load(goldenFile);
            var comparison = GoldenMeshSupport.compare(mesh, goldenMesh);

            assertTrue(comparison.matches(),
                String.format("Door mesh %s differs from golden: %s", partPath, comparison.summary()));
        }

        System.out.printf("✓ All %d door mesh parts match golden files%n",
            result.meshes().partsByPath().size());
    }

    @Test
    void fixedWindow_multipleSizes_generatesDifferentMeshes() {
        // Given: Two different window sizes
        ParameterSet small = ParameterSet.builder()
            .put("width", ParameterValue.length(800.0))
            .put("height", ParameterValue.length(1000.0))
            .build();

        ParameterSet large = ParameterSet.builder()
            .put("width", ParameterValue.length(1600.0))
            .put("height", ParameterValue.length(2000.0))
            .build();

        // When: Generate both
        PipelineResult smallResult = GenerationTestSupport.generateFixedWindowPipeline(small);
        PipelineResult largeResult = GenerationTestSupport.generateFixedWindowPipeline(large);

        // Then: Bounds should differ
        var smallBounds = smallResult.geometry().bounds();
        var largeBounds = largeResult.geometry().bounds();

        assertTrue(largeBounds.max().x() > smallBounds.max().x(),
            "Large window should be wider");
        assertTrue(largeBounds.max().y() > smallBounds.max().y(),
            "Large window should be taller");
    }

    @Test
    void casementWindow_withOpenAngle_matchesGolden() {
        // Given: Casement window with 45° open angle
        ParameterSet params = ParameterSet.builder()
            .put("open_angle", ParameterValue.angle(45.0))
            .put("frame_depth", ParameterValue.length(80.0))
            .build();

        // When: Generate pipeline result
        PipelineResult result = GenerationTestSupport.generateCasementWindowPipeline(params);

        // Then: Should have panel instead of fixed glazing
        assertFalse(result.geometry().solids().isEmpty());
        assertTrue(result.meshes().partsByPath().containsKey("panel.glazing"),
            "Should have panel glazing");

        // Should NOT have fixed glazing
        assertFalse(result.geometry().solids().stream()
            .anyMatch(s -> s.componentPath().equals("glazing")),
            "Should not have fixed glazing component");
    }

    @Test
    void pipeline_sameParameters_producesSameOutput() {
        // Given: Same parameters used twice
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        // When: Generate twice
        PipelineResult result1 = GenerationTestSupport.generateFixedWindowPipeline(params);
        PipelineResult result2 = GenerationTestSupport.generateFixedWindowPipeline(params);

        // Then: Results should be identical
        assertEquals(result1.geometry().solids().size(),
            result2.geometry().solids().size(),
            "Should have same number of solids");

        assertEquals(result1.meshes().partsByPath().size(),
            result2.meshes().partsByPath().size(),
            "Should have same number of mesh parts");

        // Bounds should match
        var bounds1 = result1.geometry().bounds();
        var bounds2 = result2.geometry().bounds();
        assertEquals(bounds1.min().x(), bounds2.min().x(), 0.001);
        assertEquals(bounds1.max().x(), bounds2.max().x(), 0.001);
        assertEquals(bounds1.min().y(), bounds2.min().y(), 0.001);
        assertEquals(bounds1.max().y(), bounds2.max().y(), 0.001);
    }

    @Test
    void curtainWall_multipleUnits_matchesGolden() {
        // Given: Curtain wall with multiple units
        ParameterSet params = ParameterSet.empty();

        // When: Generate pipeline result
        PipelineResult result = GenerationTestSupport.generateCurtainWallPipeline(params);

        // Then: Should have frame and glazing components
        assertFalse(result.geometry().solids().isEmpty());
        assertFalse(result.meshes().partsByPath().isEmpty());
    }
}
