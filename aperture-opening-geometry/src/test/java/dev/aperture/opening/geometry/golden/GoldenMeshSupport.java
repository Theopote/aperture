package dev.aperture.opening.geometry.golden;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.math.Vec2d;
import dev.aperture.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for golden mesh testing.
 * Golden files store expected mesh output to verify pipeline stability.
 */
public final class GoldenMeshSupport {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double EPSILON = 0.001;

    private GoldenMeshSupport() {
    }

    /**
     * Serialize mesh to JSON for golden file.
     */
    public static String toJson(Mesh mesh) {
        JsonObject root = new JsonObject();
        root.addProperty("vertexCount", mesh.vertexCount());
        root.addProperty("faceCount", mesh.faceCount());

        // Store vertices
        List<JsonObject> vertices = new ArrayList<>();
        for (int i = 0; i < mesh.vertexCount(); i++) {
            Vec3d v = mesh.vertex(i);
            Vec2d uv = mesh.uv(i);
            JsonObject vertex = new JsonObject();
            vertex.addProperty("x", v.x());
            vertex.addProperty("y", v.y());
            vertex.addProperty("z", v.z());
            vertex.addProperty("u", uv.u());
            vertex.addProperty("v", uv.v());
            vertices.add(vertex);
        }
        root.add("vertices", GSON.toJsonTree(vertices));

        // Store faces (indices)
        List<int[]> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faceCount(); i++) {
            faces.add(new int[]{
                mesh.faceVertex(i, 0),
                mesh.faceVertex(i, 1),
                mesh.faceVertex(i, 2)
            });
        }
        root.add("faces", GSON.toJsonTree(faces));

        return GSON.toJson(root);
    }

    /**
     * Deserialize mesh from JSON golden file.
     */
    public static GoldenMesh fromJson(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        int vertexCount = root.get("vertexCount").getAsInt();
        int faceCount = root.get("faceCount").getAsInt();

        List<Vec3d> vertices = new ArrayList<>();
        List<Vec2d> uvs = new ArrayList<>();
        for (var vertexObj : root.getAsJsonArray("vertices")) {
            JsonObject v = vertexObj.getAsJsonObject();
            vertices.add(new Vec3d(
                v.get("x").getAsDouble(),
                v.get("y").getAsDouble(),
                v.get("z").getAsDouble()
            ));
            uvs.add(new Vec2d(
                v.get("u").getAsDouble(),
                v.get("v").getAsDouble()
            ));
        }

        List<int[]> faces = new ArrayList<>();
        for (var faceObj : root.getAsJsonArray("faces")) {
            var faceArray = faceObj.getAsJsonArray();
            faces.add(new int[]{
                faceArray.get(0).getAsInt(),
                faceArray.get(1).getAsInt(),
                faceArray.get(2).getAsInt()
            });
        }

        return new GoldenMesh(vertexCount, faceCount, vertices, uvs, faces);
    }

    /**
     * Load golden mesh from file.
     */
    public static GoldenMesh load(Path path) throws IOException {
        String json = Files.readString(path);
        return fromJson(json);
    }

    /**
     * Save golden mesh to file.
     */
    public static void save(Path path, Mesh mesh) throws IOException {
        String json = toJson(mesh);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

    /**
     * Compare two meshes for equality within epsilon.
     */
    public static MeshComparison compare(Mesh actual, GoldenMesh expected) {
        List<String> differences = new ArrayList<>();

        // Check counts
        if (actual.vertexCount() != expected.vertexCount()) {
            differences.add(String.format(
                "Vertex count mismatch: expected %d, got %d",
                expected.vertexCount(), actual.vertexCount()
            ));
        }

        if (actual.faceCount() != expected.faceCount()) {
            differences.add(String.format(
                "Face count mismatch: expected %d, got %d",
                expected.faceCount(), actual.faceCount()
            ));
        }

        // Compare vertices
        int minVertices = Math.min(actual.vertexCount(), expected.vertexCount());
        for (int i = 0; i < minVertices; i++) {
            Vec3d actualV = actual.vertex(i);
            Vec3d expectedV = expected.vertices().get(i);

            if (!vecEquals(actualV, expectedV, EPSILON)) {
                differences.add(String.format(
                    "Vertex %d differs: expected (%.3f, %.3f, %.3f), got (%.3f, %.3f, %.3f)",
                    i,
                    expectedV.x(), expectedV.y(), expectedV.z(),
                    actualV.x(), actualV.y(), actualV.z()
                ));

                // Only report first 5 vertex differences to avoid spam
                if (differences.size() >= 10) {
                    differences.add("... (more differences omitted)");
                    break;
                }
            }
        }

        // Compare faces (only if vertex counts match)
        if (actual.vertexCount() == expected.vertexCount()) {
            int minFaces = Math.min(actual.faceCount(), expected.faceCount());
            for (int i = 0; i < minFaces; i++) {
                int[] actualFace = new int[]{
                    actual.faceVertex(i, 0),
                    actual.faceVertex(i, 1),
                    actual.faceVertex(i, 2)
                };
                int[] expectedFace = expected.faces().get(i);

                if (!faceEquals(actualFace, expectedFace)) {
                    differences.add(String.format(
                        "Face %d differs: expected [%d, %d, %d], got [%d, %d, %d]",
                        i,
                        expectedFace[0], expectedFace[1], expectedFace[2],
                        actualFace[0], actualFace[1], actualFace[2]
                    ));

                    if (differences.size() >= 10) {
                        differences.add("... (more differences omitted)");
                        break;
                    }
                }
            }
        }

        return new MeshComparison(differences.isEmpty(), differences);
    }

    private static boolean vecEquals(Vec3d a, Vec3d b, double epsilon) {
        return Math.abs(a.x() - b.x()) < epsilon &&
               Math.abs(a.y() - b.y()) < epsilon &&
               Math.abs(a.z() - b.z()) < epsilon;
    }

    private static boolean faceEquals(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    /**
     * Golden mesh data loaded from file.
     */
    public record GoldenMesh(
        int vertexCount,
        int faceCount,
        List<Vec3d> vertices,
        List<Vec2d> uvs,
        List<int[]> faces
    ) {
    }

    /**
     * Result of comparing actual mesh to golden.
     */
    public record MeshComparison(
        boolean matches,
        List<String> differences
    ) {
        public String summary() {
            if (matches) {
                return "Meshes match";
            }
            return String.format(
                "%d difference(s):\n  - %s",
                differences.size(),
                String.join("\n  - ", differences)
            );
        }
    }
}
