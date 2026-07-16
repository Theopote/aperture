package dev.aperture.opening.geometry.golden;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test to generate golden mesh files for comparison.
 * Run this test when you want to regenerate golden files after verified changes.
 */
class GenerateGoldenMeshes {

    private static final Path GOLDEN_DIR = Paths.get("src/test/resources/golden");

    @Test
    void generateFixedWindowGolden() throws IOException {
        // Given: Standard fixed window
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .put("frame_material", ParameterValue.materialRef("aperture:aluminum"))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("fixed_window_1200x1500", result.meshes());

        System.out.println("Generated golden mesh for fixed_window_1200x1500");
    }

    @Test
    void generateFixedWindowSmallGolden() throws IOException {
        // Given: Small fixed window
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(600.0))
            .put("height", ParameterValue.length(800.0))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateFixedWindowPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("fixed_window_600x800", result.meshes());

        System.out.println("Generated golden mesh for fixed_window_600x800");
    }

    @Test
    void generateDoorSinglePanelGolden() throws IOException {
        // Given: Standard single panel door
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("thickness", ParameterValue.length(50.0))
            .put("panel_count", ParameterValue.count(1))
            .put("glass_ratio", ParameterValue.number(0.3))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("door_single_900x2100", result.meshes());

        System.out.println("Generated golden mesh for door_single_900x2100");
    }

    @Test
    void generateDoorDoublePanelGolden() throws IOException {
        // Given: Double panel door
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1800.0))
            .put("height", ParameterValue.length(2300.0))
            .put("thickness", ParameterValue.length(60.0))
            .put("panel_count", ParameterValue.count(2))
            .put("glass_ratio", ParameterValue.number(0.4))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("door_double_1800x2300", result.meshes());

        System.out.println("Generated golden mesh for door_double_1800x2300");
    }

    @Test
    void generateDoorSolidGolden() throws IOException {
        // Given: Solid door (no glass)
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1000.0))
            .put("height", ParameterValue.length(2100.0))
            .put("panel_count", ParameterValue.count(1))
            .put("glass_ratio", ParameterValue.number(0.0))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateDoorPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("door_solid_1000x2100", result.meshes());

        System.out.println("Generated golden mesh for door_solid_1000x2100");
    }

    @Test
    void generateCurtainWallGolden() throws IOException {
        // Given: Standard curtain wall
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(3000.0))
            .put("height", ParameterValue.length(2700.0))
            .put("horizontal_divisions", ParameterValue.count(2))
            .put("vertical_divisions", ParameterValue.count(1))
            .build();

        // When: Generate through pipeline
        PipelineResult result = GenerationTestSupport.generateCurtainWallPipeline(params);

        // Then: Save golden files
        saveGoldenMeshes("curtain_wall_3000x2700", result.meshes());

        System.out.println("Generated golden mesh for curtain_wall_3000x2700");
    }

    /**
     * Saves all meshes in the assembly as individual golden JSON files.
     */
    private void saveGoldenMeshes(String prefix, MeshAssembly assembly) throws IOException {
        Files.createDirectories(GOLDEN_DIR);

        for (var entry : assembly.partsByPath().entrySet()) {
            String partPath = entry.getKey();
            Mesh mesh = entry.getValue();

            // Sanitize path for filename: replace . and / with _
            String sanitizedPath = partPath.replace('.', '_').replace('/', '_');
            String filename = prefix + "_" + sanitizedPath + ".json";
            Path goldenFile = GOLDEN_DIR.resolve(filename);

            String json = GoldenMeshSupport.toJson(mesh);
            Files.writeString(goldenFile, json);

            System.out.printf("  Wrote %s (%d vertices, %d faces)%n",
                filename, mesh.vertexCount(), mesh.faceCount());
        }

        System.out.printf("Saved %d mesh parts for %s%n",
            assembly.partsByPath().size(), prefix);
    }

    /**
     * Generate all golden meshes at once.
     */
    @Test
    void generateAllGoldenMeshes() throws IOException {
        System.out.println("=== Generating All Golden Meshes ===\n");

        generateFixedWindowGolden();
        System.out.println();

        generateFixedWindowSmallGolden();
        System.out.println();

        generateDoorSinglePanelGolden();
        System.out.println();

        generateDoorDoublePanelGolden();
        System.out.println();

        generateDoorSolidGolden();
        System.out.println();

        generateCurtainWallGolden();

        System.out.println("\n=== All Golden Meshes Generated ===");
    }
}
