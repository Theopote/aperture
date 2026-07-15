package dev.aperture.geometry.export.gltf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.recipe.GeometryRecipeBuilder;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.math.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfExporterTest {
	@Test
	void exportsSingleBoxMeshAsGltfJson() {
		var mesh = ShapeMesher.meshLocal(new BoxShape(BoundingBox.fromSize(1000, 1200, 80)));
		MeshAssembly assembly = new MeshAssembly(
			java.util.Map.of("frame.bottom", mesh),
			BoundingBox.fromSize(1000, 1200, 80)
		);

		JsonObject gltf = JsonParser.parseString(GltfExporter.export(assembly)).getAsJsonObject();

		assertEquals("2.0", gltf.getAsJsonObject("asset").get("version").getAsString());
		assertEquals(1, gltf.getAsJsonArray("meshes").size());
		assertEquals(1, gltf.getAsJsonArray("nodes").size());
		assertEquals("frame.bottom", gltf.getAsJsonArray("nodes").get(0).getAsJsonObject().get("name").getAsString());
		assertTrue(gltf.getAsJsonArray("buffers").get(0).getAsJsonObject().get("uri").getAsString().startsWith("data:application/octet-stream;base64,"));
	}

	@Test
	void exportsRecipeWithoutOpeningDomain() {
		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT,
			ShapeRecipes.box(BoundingBox.fromSize(800, 1200, 10))
		);
		builder.setCutVolume(BoundingBox.fromSize(800, 1200, 200));

		JsonObject root = JsonParser.parseString(GltfExporter.export(builder.build())).getAsJsonObject();
		assertEquals(1, root.getAsJsonArray("meshes").size());
		assertEquals(3, root.getAsJsonArray("accessors").size());
	}

	@Test
	void recipeExportTriangleCountMatchesDirectMesh() {
		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT,
			ShapeRecipes.box(BoundingBox.fromSize(800, 1200, 10))
		);
		builder.setCutVolume(BoundingBox.fromSize(800, 1200, 200));

		var geometry = GeometryRecipeExecutor.execute(builder.build());
		int directTriangles = ShapeMesher.mesh(
			geometry.solids().getFirst().shape(),
			geometry.solids().getFirst().localTransform()
		).triangleCount();

		JsonObject gltf = JsonParser.parseString(GltfExporter.export(builder.build())).getAsJsonObject();
		int indexCount = gltf.getAsJsonArray("accessors").get(2).getAsJsonObject().get("count").getAsInt();
		assertEquals(directTriangles * 3, indexCount);
	}
}
