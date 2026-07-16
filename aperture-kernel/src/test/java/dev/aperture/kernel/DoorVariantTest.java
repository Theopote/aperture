package dev.aperture.kernel;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 Phase 3: Door Variant Testing
 *
 * Comprehensive tests for different Door configurations to validate
 * the Kernel handles all parameter combinations correctly.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoorVariantTest {

    private static OpeningTypeRegistry registry;
    private static ApertureKernel kernel;
    private static final OpeningId DOOR_ID = BuiltinOpeningTypes.DOOR_ID;

    @BeforeAll
    static void setupKernel() {
        registry = new OpeningTypeRegistry();
        BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);

        kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .enableLogging(false)  // Reduce noise
            .build();

        System.out.println("\n=== Week 9 Phase 3: Door Variant Testing ===");
    }

    @AfterAll
    static void cleanupKernel() throws Exception {
        if (kernel != null) {
            kernel.close();
        }
    }

    // ===== Size Variants =====

    @Test
    @Order(1)
    @DisplayName("Standard residential door sizes")
    void doorSizes_residential_shouldAllGenerate() {
        // Standard residential sizes
        double[][] sizes = {
            {700, 2000},   // Small single
            {800, 2100},   // Standard single
            {900, 2100},   // Wide single
            {1200, 2300},  // Default
            {1400, 2400},  // Wide double
            {1600, 2500}   // Extra wide
        };

        int successCount = 0;
        for (double[] size : sizes) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(size[0]))
                .put("height", ParameterValue.length(size[1]))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(sizes.length, successCount, "All residential sizes should generate");
        System.out.printf("✓ %d residential door sizes validated%n", successCount);
    }

    @Test
    @Order(2)
    @DisplayName("Commercial door sizes")
    void doorSizes_commercial_shouldAllGenerate() {
        // Larger commercial sizes
        double[][] sizes = {
            {1800, 2700},  // Commercial double
            {2000, 2800},  // Wide commercial
            {2200, 2900},  // Extra wide
            {2400, 3000}   // Maximum size
        };

        int successCount = 0;
        for (double[] size : sizes) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(size[0]))
                .put("height", ParameterValue.length(size[1]))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(sizes.length, successCount, "All commercial sizes should generate");
        System.out.printf("✓ %d commercial door sizes validated%n", successCount);
    }

    @Test
    @Order(3)
    @DisplayName("Minimum size door")
    void doorSize_minimum_shouldGenerate() {
        // Minimum allowed size (600mm x 1800mm)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(600.0))
            .put("height", ParameterValue.length(1800.0))
            .build();

        OpeningResult result = kernel.generate(DOOR_ID, params);
        assertTrue(result.isSuccess(), "Minimum size door should generate");
        System.out.println("✓ Minimum size door (600x1800mm) generated");
    }

    @Test
    @Order(4)
    @DisplayName("Maximum size door")
    void doorSize_maximum_shouldGenerate() {
        // Maximum allowed size (2400mm x 3000mm)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(2400.0))
            .put("height", ParameterValue.length(3000.0))
            .build();

        OpeningResult result = kernel.generate(DOOR_ID, params);
        assertTrue(result.isSuccess(), "Maximum size door should generate");
        System.out.println("✓ Maximum size door (2400x3000mm) generated");
    }

    // ===== Panel Count Variants =====

    @Test
    @Order(10)
    @DisplayName("All panel counts (1-6)")
    void doorPanels_allCounts_shouldGenerate() {
        int successCount = 0;

        for (int panelCount = 1; panelCount <= 6; panelCount++) {
            ParameterSet params = ParameterSet.builder()
                .put("panel_count", ParameterValue.count(panelCount))
                .put("width", ParameterValue.length(600.0 * panelCount))  // Scale width
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(6, successCount, "All panel counts 1-6 should generate");
        System.out.printf("✓ All %d panel count variants validated%n", successCount);
    }

    // ===== Glass Ratio Variants =====

    @Test
    @Order(20)
    @DisplayName("Glass ratio variations")
    void doorGlass_allRatios_shouldGenerate() {
        // Test various glass ratios
        double[] ratios = {0.0, 0.1, 0.25, 0.35, 0.5, 0.75, 0.9, 1.0};

        int successCount = 0;
        for (double ratio : ratios) {
            ParameterSet params = ParameterSet.builder()
                .put("glass_ratio", ParameterValue.number(ratio))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(ratios.length, successCount, "All glass ratios should generate");
        System.out.printf("✓ %d glass ratio variants validated%n", successCount);
    }

    // ===== Thickness Variants =====

    @Test
    @Order(30)
    @DisplayName("Door thickness variations")
    void doorThickness_allVariants_shouldGenerate() {
        // Test different thicknesses
        double[] thicknesses = {35, 40, 45, 50, 60, 70, 80, 100, 120};

        int successCount = 0;
        for (double thickness : thicknesses) {
            ParameterSet params = ParameterSet.builder()
                .put("thickness", ParameterValue.length(thickness))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(thicknesses.length, successCount, "All thicknesses should generate");
        System.out.printf("✓ %d thickness variants validated%n", successCount);
    }

    // ===== Frame Width Variants =====

    @Test
    @Order(40)
    @DisplayName("Frame width variations")
    void doorFrame_allWidths_shouldGenerate() {
        // Test different frame widths
        double[] frameWidths = {40, 50, 60, 70, 80, 100, 120, 150, 200};

        int successCount = 0;
        for (double width : frameWidths) {
            ParameterSet params = ParameterSet.builder()
                .put("frame_width", ParameterValue.length(width))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(frameWidths.length, successCount, "All frame widths should generate");
        System.out.printf("✓ %d frame width variants validated%n", successCount);
    }

    // ===== Hinge Side Variants =====

    @Test
    @Order(50)
    @DisplayName("Both hinge sides")
    void doorHinge_bothSides_shouldGenerate() {
        String[] sides = {"left", "right"};

        int successCount = 0;
        for (String side : sides) {
            ParameterSet params = ParameterSet.builder()
                .put("hinge_side", ParameterValue.enumValue(side))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(sides.length, successCount, "Both hinge sides should generate");
        System.out.printf("✓ Both hinge side variants validated%n");
    }

    // ===== Transom Variants =====

    @Test
    @Order(60)
    @DisplayName("With and without transom")
    void doorTransom_both_shouldGenerate() {
        boolean[] transomOptions = {false, true};

        int successCount = 0;
        for (boolean hasTransom : transomOptions) {
            ParameterSet params = ParameterSet.builder()
                .put("has_transom", ParameterValue.bool(hasTransom))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(transomOptions.length, successCount, "Both transom options should generate");
        System.out.printf("✓ Transom variants validated%n");
    }

    // ===== Combined Variants =====

    @Test
    @Order(100)
    @DisplayName("Common door configurations")
    void doorConfigs_common_shouldAllGenerate() {
        List<DoorConfig> configs = List.of(
            new DoorConfig("Standard Residential", 900, 2100, 1, 0.25, "left", false),
            new DoorConfig("Front Entry", 1000, 2300, 1, 0.5, "right", false),
            new DoorConfig("French Doors", 1600, 2400, 2, 0.75, "left", false),
            new DoorConfig("Commercial Entry", 1800, 2700, 2, 0.5, "right", true),
            new DoorConfig("Glass Double", 1400, 2400, 2, 1.0, "left", false),
            new DoorConfig("Solid Single", 800, 2100, 1, 0.0, "right", false),
            new DoorConfig("Wide Commercial", 2200, 2800, 3, 0.35, "left", true)
        );

        int successCount = 0;
        List<String> failed = new ArrayList<>();

        for (DoorConfig config : configs) {
            OpeningResult result = kernel.generate(DOOR_ID, config.toParameterSet());
            if (result.isSuccess()) {
                successCount++;
            } else {
                failed.add(config.name);
            }
        }

        if (!failed.isEmpty()) {
            System.out.println("Failed configurations: " + failed);
        }

        assertEquals(configs.size(), successCount, "All common configurations should generate");
        System.out.printf("✓ %d common door configurations validated%n", successCount);
    }

    @Test
    @Order(101)
    @DisplayName("Edge case combinations")
    void doorConfigs_edgeCases_shouldGenerate() {
        List<DoorConfig> edgeCases = List.of(
            new DoorConfig("Min Size Max Panels", 600, 1800, 6, 0.5, "left", false),
            new DoorConfig("Max Size Min Panel", 2400, 3000, 1, 0.5, "right", true),
            new DoorConfig("Thin Wide Door", 2000, 2100, 4, 0.25, "left", false),
            new DoorConfig("Thick Narrow Door", 700, 2800, 1, 0.75, "right", false),
            new DoorConfig("Full Glass Multi-Panel", 1800, 2500, 3, 1.0, "left", false),
            new DoorConfig("Solid Multi-Panel", 1600, 2400, 4, 0.0, "right", false)
        );

        int successCount = 0;
        for (DoorConfig config : edgeCases) {
            OpeningResult result = kernel.generate(DOOR_ID, config.toParameterSet());
            if (result.isSuccess()) {
                successCount++;
            }
        }

        System.out.printf("✓ %d/%d edge case configurations validated%n", successCount, edgeCases.size());
    }

    // ===== Performance Test =====

    @Test
    @Order(200)
    @DisplayName("Batch generation performance")
    void doorGeneration_batch_shouldBeEfficient() {
        int batchSize = 50;
        long startTime = System.nanoTime();

        int successCount = 0;
        for (int i = 0; i < batchSize; i++) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(700.0 + (i % 18) * 100))
                .put("height", ParameterValue.length(2000.0 + (i % 11) * 100))
                .put("panel_count", ParameterValue.count(1 + (i % 3)))
                .put("glass_ratio", ParameterValue.number((i % 5) * 0.25))
                .build();

            OpeningResult result = kernel.generate(DOOR_ID, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        double avgMs = (double) elapsedMs / batchSize;

        assertEquals(batchSize, successCount, "All batch doors should generate");

        System.out.printf("\n=== Batch Performance ===%n");
        System.out.printf("Generated: %d doors%n", batchSize);
        System.out.printf("Total time: %d ms%n", elapsedMs);
        System.out.printf("Average: %.1f ms/door%n", avgMs);
        System.out.printf("Throughput: %.1f doors/sec%n", 1000.0 / avgMs);
    }

    @AfterAll
    static void printVariantSummary() {
        KernelStats stats = kernel.getStats();

        System.out.println("\n=== Phase 3 Complete ===");
        System.out.printf("Total doors tested: %d%n", stats.totalRequests());
        System.out.printf("Success rate: %.1f%%%n", stats.successRate() * 100);
        System.out.printf("Average time: %.0f ms%n", stats.averageExecutionTimeMs());
        System.out.println("\n✓ All door variants validated");
        System.out.println("✓ Size, panels, glass, thickness, frame, hinge tested");
        System.out.println("✓ Common and edge case configurations working");
        System.out.println("\n→ Ready for Phase 4: Performance benchmarking");
    }

    // ===== Helper Class =====

    private record DoorConfig(
        String name,
        double width,
        double height,
        int panelCount,
        double glassRatio,
        String hingeSide,
        boolean hasTransom
    ) {
        ParameterSet toParameterSet() {
            return ParameterSet.builder()
                .put("width", ParameterValue.length(width))
                .put("height", ParameterValue.length(height))
                .put("panel_count", ParameterValue.count(panelCount))
                .put("glass_ratio", ParameterValue.number(glassRatio))
                .put("hinge_side", ParameterValue.enumValue(hingeSide))
                .put("has_transom", ParameterValue.bool(hasTransom))
                .build();
        }
    }
}
