package dev.aperture.geometry.export;

import dev.aperture.geometry.export.gltf.GltfExporter;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.pipeline.mesh.MeshAssembler;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;

/**
 * Kernel-only export helpers: recipe JSON and glTF mesh export without opening-domain types.
 */
public final class GeometryExport {
	private GeometryExport() {
	}

	public static String recipeToJson(GeometryRecipe recipe) {
		return GeometryRecipeCodec.toJson(recipe);
	}

	public static GeometryRecipe recipeFromJson(String json) {
		return GeometryRecipeCodec.fromJson(json);
	}

	public static String toGltf(GeometryRecipe recipe) {
		return GltfExporter.export(recipe);
	}

	public static String toGltf(GeometryResult geometry) {
		return GltfExporter.export(new MeshAssembler().assemble(geometry));
	}

	public static String toGltf(MeshAssembly meshes) {
		return GltfExporter.export(meshes);
	}
}
