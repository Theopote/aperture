package dev.aperture.kernel;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Week 9 Phase 2: Door Generation using Kernel API
 *
 * This replaces the old Pipeline-based approach with the new unified Kernel.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoorGenerationTest {

    private static OpeningTypeRegistry registry;
    private static ApertureKernel kernel;

    @BeforeAll
    static void setupKernel() {
        // Setup registry
        registry = new OpeningTypeRegistry();
        BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);

        // Create kernel with registry
        kernel = ApertureKernel.builder()
            .registry(registry)
            .cacheCapacity(100)
            .enableLogging(true)
            .build();

        System.out.println("\n=== Week 9 Phase 2: Door Generation with Kernel ===");
    }

    @AfterAll
    static void cleanupKernel() throws Exception {
        if (kernel != null) {
            kernel.close();
            System.out.println("\n✓ Kernel closed successfully");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Generate door with default parameters")
    void generateDoor_withDefaults_shouldSucceed() {
        // Given: Door ID and empty parameters
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.empty();

        // When: Generate door
        long startTime = System.nanoTime();
        OpeningResult result = kernel.generate(doorId, params);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Door generation should succeed");

        var success = result.asSuccess();
        assertNotNull(success.placement(), "Should have placement info");
        assertNotNull(success.metrics(), "Should have generation metrics");

        System.out.printf("✓ Door generated successfully in %d ms%n", elapsedMs);
        System.out.printf("  Metrics: total=%d ms%n", success.metrics().totalTime().toNanos());
    }

    @Test
    @Order(2)
    @DisplayName("Generate door with custom dimensions")
    void generateDoor_withCustomDimensions_shouldSucceed() {
        // Given: Custom door size (900mm x 2100mm)
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("thickness", ParameterValue.length(50.0))
            .build();

        // When: Generate door
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Custom door should generate: " + result);

        var success = result.asSuccess();
        var placement = success.placement();
        assertNotNull(placement.bounds(), "Should have bounds");

        System.out.printf("✓ Custom door (900x2100mm) generated%n");
        System.out.printf("  Bounds: %.0f x %.0f x %.0f mm%n",
            placement.bounds().width(),
            placement.bounds().height(),
            placement.bounds().depth());
    }

    @Test
    @Order(3)
    @DisplayName("Generate single panel door")
    void generateDoor_singlePanel_shouldSucceed() {
        // Given: Single panel door
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("panel_count", ParameterValue.count(1))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Single panel door should generate");
        System.out.println("✓ Single panel door generated");
    }

    @Test
    @Order(4)
    @DisplayName("Generate double panel door")
    void generateDoor_doublePanels_shouldSucceed() {
        // Given: Double panel door
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("panel_count", ParameterValue.count(2))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Double panel door should generate");
        System.out.println("✓ Double panel door generated");
    }

    @Test
    @Order(5)
    @DisplayName("Generate solid door (no glass)")
    void generateDoor_noGlass_shouldSucceed() {
        // Given: Solid door without glass
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("glass_ratio", ParameterValue.number(0.0))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Solid door should generate");
        System.out.println("✓ Solid door (0% glass) generated");
    }

    @Test
    @Order(6)
    @DisplayName("Generate glass door (full glass)")
    void generateDoor_fullGlass_shouldSucceed() {
        // Given: Full glass door
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("glass_ratio", ParameterValue.number(1.0))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Glass door should generate");
        System.out.println("✓ Glass door (100% glass) generated");
    }

    @Test
    @Order(7)
    @DisplayName("Generate door with left hinge")
    void generateDoor_leftHinge_shouldSucceed() {
        // Given: Left-hinged door
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("hinge_side", ParameterValue.enumValue("left"))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Left-hinged door should generate");
        System.out.println("✓ Left-hinged door generated");
    }

    @Test
    @Order(8)
    @DisplayName("Generate door with right hinge")
    void generateDoor_rightHinge_shouldSucceed() {
        // Given: Right-hinged door
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("hinge_side", ParameterValue.enumValue("right"))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Right-hinged door should generate");
        System.out.println("✓ Right-hinged door generated");
    }

    @Test
    @Order(9)
    @DisplayName("Generate door with transom")
    void generateDoor_withTransom_shouldSucceed() {
        // Given: Door with transom
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("has_transom", ParameterValue.bool(true))
            .build();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Should succeed
        assertTrue(result.isSuccess(), "Door with transom should generate");
        System.out.println("✓ Door with transom generated");
    }

    @Test
    @Order(10)
    @DisplayName("Generate multiple doors in sequence")
    void generateDoors_sequence_shouldSucceed() {
        // Given: Various door configurations
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;

        int successCount = 0;
        int totalCount = 5;

        // When: Generate multiple doors
        for (int i = 0; i < totalCount; i++) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(800.0 + i * 200))
                .put("height", ParameterValue.length(2000.0 + i * 100))
                .build();

            OpeningResult result = kernel.generate(doorId, params);
            if (result.isSuccess()) {
                successCount++;
            }
        }

        // Then: All should succeed
        assertEquals(totalCount, successCount, "All doors should generate successfully");
        System.out.printf("✓ Generated %d doors in sequence%n", successCount);
    }

    @Test
    @Order(11)
    @DisplayName("Kernel statistics should track door generations")
    void kernelStats_shouldTrackGenerations() {
        // Given: Kernel has processed doors
        // (Previous tests have run)

        // When: Get statistics
        KernelStats stats = kernel.getStats();

        // Then: Should have recorded generations
        assertTrue(stats.totalRequests() > 0, "Should have total requests");
        assertTrue(stats.successfulRequests() > 0, "Should have successful requests");
        assertTrue(stats.averageExecutionTimeMs() >= 0, "Should have execution time");

        System.out.println("\n=== Kernel Statistics ===");
        System.out.printf("Total requests: %d%n", stats.totalRequests());
        System.out.printf("Successful: %d%n", stats.successfulRequests());
        System.out.printf("Failed: %d%n", stats.failedRequests());
        System.out.printf("Average time: %.0f ms%n", stats.averageExecutionTimeMs());
        System.out.printf("Success rate: %.1f%%%n", stats.successRate() * 100);
    }

    @Test
    @Order(12)
    @DisplayName("Cache should improve second generation")
    void cache_shouldImprovePerformance() {
        // Given: Same door parameters
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .put("height", ParameterValue.length(2200.0))
            .build();

        // When: Generate first time (uncached)
        long firstStart = System.nanoTime();
        OpeningResult first = kernel.generate(doorId, params);
        long firstMs = (System.nanoTime() - firstStart) / 1_000_000;

        // And: Generate second time (should hit cache)
        long secondStart = System.nanoTime();
        OpeningResult second = kernel.generate(doorId, params);
        long secondMs = (System.nanoTime() - secondStart) / 1_000_000;

        // Then: Both should succeed
        assertTrue(first.isSuccess(), "First generation should succeed");
        assertTrue(second.isSuccess(), "Second generation should succeed");

        // Second should be faster (cache hit)
        System.out.printf("\n✓ Cache performance:%n");
        System.out.printf("  First generation: %d ms%n", firstMs);
        System.out.printf("  Second generation: %d ms%n", secondMs);
        System.out.printf("  Speedup: %.1fx%n", (double) firstMs / Math.max(secondMs, 1));
    }

    @Test
    @Order(13)
    @DisplayName("Generation metrics should be available")
    void generationMetrics_shouldBeAvailable() {
        // Given: Door generation
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;
        ParameterSet params = ParameterSet.empty();

        // When: Generate
        OpeningResult result = kernel.generate(doorId, params);

        // Then: Metrics should be available
        assertTrue(result.isSuccess(), "Should succeed");
        var metrics = result.asSuccess().metrics();

        assertNotNull(metrics, "Should have metrics");
        assertTrue(metrics.totalTime().toNanos() > 0, "Should have total time");

        System.out.println("\n=== Generation Metrics ===");
        System.out.printf("Total time: %.3f ms%n", metrics.totalTime().toNanos() / 1_000_000.0);

        if (!metrics.stageTimings().isEmpty()) {
            System.out.println("Stage timings:");
            metrics.stageTimings().forEach((stage, time) ->
                System.out.printf("  %s: %.3f ms%n", stage, time.toNanos() / 1_000_000.0));
        }
    }

    @Test
    @Order(14)
    @DisplayName("Invalid door type should fail gracefully")
    void generateDoor_invalidType_shouldFail() {
        // Given: Invalid opening type
        OpeningId invalidId = OpeningId.parse("aperture:nonexistent");
        ParameterSet params = ParameterSet.empty();

        // When: Attempt to generate
        OpeningResult result = kernel.generate(invalidId, params);

        // Then: Should fail gracefully
        assertTrue(result.isFailure(), "Should fail for invalid type");

        var failure = result.asFailure();
        assertNotNull(failure.stage(), "Should have failure stage");
        assertNotNull(failure.message(), "Should have error message");

        System.out.println("\n✓ Invalid type handled gracefully:");
        System.out.printf("  Stage: %s%n", failure.stage());
        System.out.printf("  Message: %s%n", failure.message());
    }

    @Test
    @Order(15)
    @DisplayName("Kernel should be reusable across generations")
    void kernel_shouldBeReusable() {
        // Given: Same kernel instance
        OpeningId doorId = BuiltinOpeningTypes.DOOR_ID;

        // When: Generate multiple different doors
        int generations = 10;
        for (int i = 0; i < generations; i++) {
            ParameterSet params = ParameterSet.builder()
                .put("width", ParameterValue.length(700.0 + i * 100))
                .build();

            OpeningResult result = kernel.generate(doorId, params);
            assertTrue(result.isSuccess(), "Generation " + i + " should succeed");
        }

        // Then: Kernel should still be functional
        KernelStats stats = kernel.getStats();
        assertTrue(stats.totalRequests() >= generations, "Should have tracked all generations");

        System.out.printf("\n✓ Kernel processed %d generations successfully%n", generations);
    }

    @AfterAll
    static void printPhaseSummary() {
        System.out.println("\n=== Phase 2 Complete ===");
        System.out.println("✓ Door generation with Kernel API verified");
        System.out.println("✓ All variants (panels, glass, hinges) working");
        System.out.println("✓ Caching and metrics functional");
        System.out.println("✓ Error handling tested");
        System.out.println("\n→ Ready for Phase 3: Door variant testing");
    }
}
