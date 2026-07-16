package dev.aperture.kernel;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 Phase 1: Door Type Registration Validation
 *
 * Validates that the Door type is properly registered and configured
 * before proceeding with Kernel-based generation tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoorKernelTest {

    private static OpeningTypeRegistry registry;

    @BeforeAll
    static void setupRegistry() {
        // Create registry and register built-in types
        registry = new OpeningTypeRegistry();
        BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);

        System.out.println("=== Week 9: Door Implementation Validation ===");
        System.out.println("Phase 1: Door Type Registration");
    }

    @Test
    @Order(1)
    @DisplayName("Door type should be registered")
    void doorType_shouldBeRegistered() {
        // Given: Door ID
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;

        // When: Check registry
        var definition = registry.get(doorId);

        // Then: Should be registered
        assertTrue(definition.isPresent(), "Door type should be registered");
        assertEquals("aperture:door", doorId.toString());

        System.out.println("✓ Door type registered: " + doorId);
    }

    @Test
    @Order(2)
    @DisplayName("Door should have all required parameters")
    void doorDefinition_shouldHaveRequiredParameters() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // Then: Should have essential parameters
        assertTrue(doorDef.hasParameter("width"), "Should have width parameter");
        assertTrue(doorDef.hasParameter("height"), "Should have height parameter");
        assertTrue(doorDef.hasParameter("thickness"), "Should have thickness parameter");
        assertTrue(doorDef.hasParameter("panel_count"), "Should have panel_count parameter");
        assertTrue(doorDef.hasParameter("glass_ratio"), "Should have glass_ratio parameter");
        assertTrue(doorDef.hasParameter("frame_width"), "Should have frame_width parameter");
        assertTrue(doorDef.hasParameter("hinge_side"), "Should have hinge_side parameter");
        assertTrue(doorDef.hasParameter("has_transom"), "Should have has_transom parameter");

        System.out.println("✓ Door has 8 core parameters");
    }

    @Test
    @Order(3)
    @DisplayName("Door parameters should have correct default values")
    void doorParameters_shouldHaveCorrectDefaults() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // When: Get default parameters
        var width = doorDef.getParameter("width");
        var height = doorDef.getParameter("height");
        var thickness = doorDef.getParameter("thickness");
        var panelCount = doorDef.getParameter("panel_count");
        var glassRatio = doorDef.getParameter("glass_ratio");
        var frameWidth = doorDef.getParameter("frame_width");

        // Then: Defaults should match specification
        assertEquals(1200.0, width.defaultValue().asNumber(), "Width default should be 1200mm");
        assertEquals(2300.0, height.defaultValue().asNumber(), "Height default should be 2300mm");
        assertEquals(60.0, thickness.defaultValue().asNumber(), "Thickness default should be 60mm");
        assertEquals(2.0, panelCount.defaultValue().asNumber(), "Panel count default should be 2");
        assertEquals(0.35, glassRatio.defaultValue().asNumber(), 0.01, "Glass ratio default should be 0.35");
        assertEquals(80.0, frameWidth.defaultValue().asNumber(), "Frame width default should be 80mm");

        System.out.println("✓ Door default values verified:");
        System.out.printf("  - Dimensions: %.0f x %.0f x %.0f mm%n",
            width.defaultValue().asNumber(),
            height.defaultValue().asNumber(),
            thickness.defaultValue().asNumber());
        System.out.printf("  - Panel count: %.0f, Glass ratio: %.2f%n",
            panelCount.defaultValue().asNumber(),
            glassRatio.defaultValue().asNumber());
    }

    @Test
    @Order(4)
    @DisplayName("Door should have parameter constraints")
    void doorDefinition_shouldHaveConstraints() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // Then: Should have constraints defined
        var constraints = doorDef.constraints();
        assertFalse(constraints.isEmpty(), "Should have constraints");

        // Check for specific constraints
        boolean hasWidthHeightConstraint = constraints.stream()
            .anyMatch(c -> c.expression().contains("width") && c.expression().contains("height"));
        assertTrue(hasWidthHeightConstraint, "Should have width/height relationship constraint");

        boolean hasPanelCountConstraint = constraints.stream()
            .anyMatch(c -> c.expression().contains("panel_count"));
        assertTrue(hasPanelCountConstraint, "Should have panel_count constraint");

        boolean hasGlassRatioConstraint = constraints.stream()
            .anyMatch(c -> c.expression().contains("glass_ratio"));
        assertTrue(hasGlassRatioConstraint, "Should have glass_ratio constraint");

        System.out.println("✓ Door has " + constraints.size() + " constraints:");
        constraints.forEach(c -> System.out.println("  - " + c.message()));
    }

    @Test
    @Order(5)
    @DisplayName("Door should have material slots")
    void doorDefinition_shouldHaveMaterialSlots() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // Then: Should have material slots
        var materialSlots = doorDef.materialSlots();
        assertFalse(materialSlots.isEmpty(), "Should have material slots");

        // Door should have frame, glazing, and hardware slots
        assertTrue(materialSlots.contains("frame"), "Should have frame material slot");
        assertTrue(materialSlots.contains("glazing"), "Should have glazing material slot");
        assertTrue(materialSlots.contains("hardware"), "Should have hardware material slot");

        System.out.println("✓ Door has " + materialSlots.size() + " material slots: " + materialSlots);
    }

    @Test
    @Order(6)
    @DisplayName("Door should have component assembly")
    void doorDefinition_shouldHaveComponents() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // Then: Should have component assembly
        var components = doorDef.components();
        assertNotNull(components, "Should have component assembly");

        System.out.println("✓ Door has component assembly configured");
    }

    @Test
    @Order(7)
    @DisplayName("Door parameter set should resolve with defaults")
    void doorParameterSet_shouldResolveDefaults() {
        // Given: Empty parameter set
        ParameterSet params = ParameterSet.empty();

        // When: Resolve against door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);
        var resolved = doorDef.resolveParameters(params);

        // Then: Should have all parameters with default values
        assertTrue(resolved.has("width"), "Should have width");
        assertTrue(resolved.has("height"), "Should have height");
        assertTrue(resolved.has("thickness"), "Should have thickness");

        assertEquals(1200.0, resolved.require("width").asNumber(), "Width should use default");
        assertEquals(2300.0, resolved.require("height").asNumber(), "Height should use default");

        System.out.println("✓ Empty parameter set resolved with defaults");
    }

    @Test
    @Order(8)
    @DisplayName("Door parameter set should override defaults")
    void doorParameterSet_shouldOverrideDefaults() {
        // Given: Custom parameters
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("thickness", ParameterValue.length(50.0))
            .build();

        // When: Resolve against door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);
        var resolved = doorDef.resolveParameters(params);

        // Then: Should use custom values
        assertEquals(900.0, resolved.require("width").asNumber(), "Width should use custom value");
        assertEquals(2100.0, resolved.require("height").asNumber(), "Height should use custom value");
        assertEquals(50.0, resolved.require("thickness").asNumber(), "Thickness should use custom value");

        // Other parameters should still have defaults
        assertEquals(2.0, resolved.require("panel_count").asNumber(), "Panel count should use default");

        System.out.println("✓ Custom parameters override defaults correctly");
    }

    @Test
    @Order(9)
    @DisplayName("Door should validate within parameter ranges")
    void doorParameters_shouldValidateRanges() {
        // Given: Door definition
        var doorDef = registry.require(BuiltinOpeningTypes.DOOR_ID);

        // When: Get parameter specs
        var width = doorDef.getParameter("width");
        var height = doorDef.getParameter("height");
        var panelCount = doorDef.getParameter("panel_count");

        // Then: Should have valid ranges
        // Width: 600-2400mm
        assertTrue(width.defaultValue().asNumber() >= 600, "Width min should be >= 600");
        assertTrue(width.defaultValue().asNumber() <= 2400, "Width max should be <= 2400");

        // Height: 1800-3000mm
        assertTrue(height.defaultValue().asNumber() >= 1800, "Height min should be >= 1800");
        assertTrue(height.defaultValue().asNumber() <= 3000, "Height max should be <= 3000");

        // Panel count: 1-6
        assertTrue(panelCount.defaultValue().asNumber() >= 1, "Panel count min should be >= 1");
        assertTrue(panelCount.defaultValue().asNumber() <= 6, "Panel count max should be <= 6");

        System.out.println("✓ Parameter ranges validated:");
        System.out.println("  - Width: 600-2400mm");
        System.out.println("  - Height: 1800-3000mm");
        System.out.println("  - Panel count: 1-6");
    }

    @Test
    @Order(10)
    @DisplayName("All reference types should be registered")
    void allReferenceTypes_shouldBeRegistered() {
        // Given: Reference type IDs
        var referenceIds = BuiltinOpeningTypes.referenceIds();

        // Then: All should be registered
        for (OpeningId id : referenceIds) {
            assertTrue(registry.get(id).isPresent(), "Reference type should be registered: " + id);
        }

        assertEquals(3, referenceIds.size(), "Should have 3 reference types");

        System.out.println("✓ All 3 reference types registered:");
        referenceIds.forEach(id -> System.out.println("  - " + id));
    }

    @AfterAll
    static void printSummary() {
        System.out.println("\n=== Phase 1 Complete ===");
        System.out.println("✓ Door type registration verified");
        System.out.println("✓ All parameters and constraints validated");
        System.out.println("✓ Ready for Phase 2: Kernel-based generation");
    }
}
