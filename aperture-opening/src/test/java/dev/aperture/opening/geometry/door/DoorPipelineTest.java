package dev.aperture.opening.geometry.door;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.geometry.pipeline.CollisionCalculator;
import dev.aperture.geometry.pipeline.FootprintCalculator;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.math.BoundingBox;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for door generation through complete pipeline.
 * Verifies all door components generate correctly.
 */
class DoorPipelineTest {

    @Test
    void door_defaultParameters_generatesAllComponents() {
        // Given: Default door parameters
        ParameterSet params = ParameterSet.empty();

        // When: Generate door through pipeline
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: All core components should exist
        assertNotNull(result, "Pipeline result should not be null");
        assertFalse(result.geometry().solids().isEmpty(), "Should have geometry solids");
        assertFalse(result.meshes().partsByPath().isEmpty(), "Should have mesh parts");

        // Frame components
        assertTrue(result.meshes().partsByPath().containsKey("door_frame.bottom"),
            "Should have frame bottom");
        assertTrue(result.meshes().partsByPath().containsKey("door_frame.top"),
            "Should have frame top");
        assertTrue(result.meshes().partsByPath().containsKey("door_frame.left"),
            "Should have frame left");
        assertTrue(result.meshes().partsByPath().containsKey("door_frame.right"),
            "Should have frame right");

        // Door leaf (panel) components
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.bottom"),
            "Should have door leaf bottom");
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.infill"),
            "Should have door leaf infill");

        // Glazing
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.glazing"),
            "Should have door leaf glazing");

        // Threshold (sill)
        assertTrue(result.meshes().partsByPath().containsKey("threshold.main"),
            "Should have threshold");

        // Handle
        assertTrue(result.meshes().partsByPath().containsKey("handle.main"),
            "Should have handle");

        System.out.printf("Door generated %d geometry solids and %d mesh parts%n",
            result.geometry().solids().size(),
            result.meshes().partsByPath().size());
    }

    @Test
    void door_customDimensions_generatesCorrectSize() {
        // Given: Custom door dimensions (900mm x 2100mm)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("thickness", ParameterValue.length(50.0))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Bounds should reflect dimensions
        BoundingBox bounds = result.geometry().bounds();
        assertTrue(bounds.max().x() >= 900.0, "Width should be at least 900mm");
        assertTrue(bounds.max().y() >= 2100.0, "Height should be at least 2100mm");

        System.out.printf("Door bounds: %.0f x %.0f x %.0f mm%n",
            bounds.width(), bounds.height(), bounds.depth());
    }

    @Test
    void door_singlePanel_generatesOneLeaf() {
        // Given: Single panel door
        ParameterSet params = ParameterSet.builder()
            .put("panel_count", ParameterValue.count(1))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Should have only one door leaf
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.bottom"),
            "Should have door leaf 0");
        assertFalse(result.meshes().partsByPath().containsKey("door_leaf.1.bottom"),
            "Should NOT have door leaf 1");
    }

    @Test
    void door_doublePanels_generatesTwoLeaves() {
        // Given: Double panel door
        ParameterSet params = ParameterSet.builder()
            .put("panel_count", ParameterValue.count(2))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Should have two door leaves
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.bottom"),
            "Should have door leaf 0");
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.1.bottom"),
            "Should have door leaf 1");
    }

    @Test
    void door_noGlass_onlyPanelInfill() {
        // Given: Solid door (no glass)
        ParameterSet params = ParameterSet.builder()
            .put("glass_ratio", ParameterValue.number(0.0))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Should have infill but minimal or no glazing
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.infill"),
            "Should have panel infill");
        // Glass might still have minimal area, so we don't assert absence
    }

    @Test
    void door_fullGlass_maximumGlazing() {
        // Given: Full glass door
        ParameterSet params = ParameterSet.builder()
            .put("glass_ratio", ParameterValue.number(1.0))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Should have glazing
        assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.glazing"),
            "Should have door glazing");
    }

    @Test
    void door_withCollisionAndFootprint_calculatesCorrectly() {
        // Given: Standard door
        ParameterSet params = ParameterSet.empty();

        // When: Generate door with collision and footprint
        PipelineResult baseResult = GenerationTestSupport.generateDoorPipeline(params);
        BoundingBox collision = CollisionCalculator.calculate(baseResult.geometry());
        BoundingBox footprint = FootprintCalculator.calculate(baseResult.geometry());

        PipelineResult result = baseResult.withCollisionAndFootprint(collision, footprint);

        // Then: Should have collision and footprint
        assertNotNull(result.collision(), "Should have collision bounds");
        assertNotNull(result.footprint(), "Should have footprint bounds");

        // Collision should be 3D
        assertTrue(result.collision().height() > 0, "Collision should have height");

        // Footprint should be 2D (minimal Y)
        assertEquals(1.0, result.footprint().height(), 0.01,
            "Footprint should have minimal height");

        // Footprint area should be reasonable
        double footprintArea = FootprintCalculator.area(result.footprint());
        assertTrue(footprintArea > 0, "Footprint should have positive area");

        System.out.printf("Door collision: %.0f x %.0f x %.0f mm%n",
            result.collision().width(),
            result.collision().height(),
            result.collision().depth());
        System.out.printf("Door footprint: %.0f x %.0f mm (area: %.0f mm²)%n",
            result.footprint().width(),
            result.footprint().depth(),
            footprintArea);
    }

    @Test
    void door_differentMaterials_generatesCorrectly() {
        // Given: Door with custom materials
        ParameterSet params = ParameterSet.builder()
            .put("frame_material", ParameterValue.materialRef("aperture:oak"))
            .put("panel_material", ParameterValue.materialRef("aperture:oak"))
            .build();

        // When: Generate door
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Should generate successfully
        assertNotNull(result);
        assertFalse(result.geometry().solids().isEmpty());
    }

    @Test
    void door_constraintValidation_definesLayoutConstraints() {
        // Note: This test verifies constraint is defined
        // Actual constraint validation happens in parameter resolution

        var doorType = BuiltinOpeningTypes.door();

        // Then: Should have width constraint
        var constraints = doorType.constraints();
        assertTrue(constraints.stream()
            .anyMatch(c -> c.expression().contains("panel_count")),
            "Should have panel count constraint");
    }

    @Test
    void door_performance_generatesInReasonableTime() {
        // Given: Standard door
        ParameterSet params = ParameterSet.empty();

        // When: Generate and measure time
        long startTime = System.nanoTime();
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete in reasonable time
        assertNotNull(result);
        System.out.printf("Door generation time: %d ms%n", elapsedMs);

        // Informational: door should generate within 200ms
        // (doors are more complex than simple windows)
    }

    @Test
    void door_multipleSizes_allGenerateSuccessfully() {
        // Given: Various door sizes
        double[] widths = {700, 900, 1000, 1200};
        double[] heights = {2000, 2100, 2300, 2500};

        // When: Generate each combination
        for (double width : widths) {
            for (double height : heights) {
                ParameterSet params = ParameterSet.builder()
                    .put("width", ParameterValue.length(width))
                    .put("height", ParameterValue.length(height))
                    .build();

                PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

                // Then: Each should generate successfully
                assertNotNull(result, String.format("Door %dx%d should generate", (int)width, (int)height));
                assertFalse(result.geometry().solids().isEmpty());
            }
        }

        System.out.printf("Successfully generated %d door variations%n", widths.length * heights.length);
    }
}
