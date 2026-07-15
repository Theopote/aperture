package dev.aperture.geometry.export.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.pipeline.mesh.MeshAssembler;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Exports {@link MeshAssembly} to glTF 2.0 JSON with an embedded binary buffer.
 * Millimeter-space meshes are converted to meters for glTF compatibility.
 */
public final class GltfExporter {
	public static final float MM_TO_METERS = 0.001f;
	public static final String GENERATOR = "Aperture Geometry Kernel";

	private GltfExporter() {
	}

	public static String export(MeshAssembly assembly) {
		return export(assembly, MM_TO_METERS);
	}

	public static String export(MeshAssembly assembly, float unitScale) {
		if (assembly.partsByPath().isEmpty()) {
			throw new IllegalArgumentException("mesh assembly is empty");
		}

		ByteArrayOutputStream binary = new ByteArrayOutputStream();
		List<JsonObject> accessors = new ArrayList<>();
		List<JsonObject> bufferViews = new ArrayList<>();
		List<JsonObject> meshes = new ArrayList<>();
		List<JsonObject> nodes = new ArrayList<>();
		JsonArray sceneNodes = new JsonArray();

		for (Map.Entry<String, Mesh> entry : assembly.partsByPath().entrySet()) {
			Mesh part = entry.getValue();

			int positionView = bufferViews.size();
			int positionOffset = binary.size();
			writePositions(binary, part, unitScale);
			bufferViews.add(bufferView(positionOffset, part.vertexCount() * 3 * Float.BYTES, 34962));
			int positionAccessor = accessors.size();
			accessors.add(vec3Accessor(positionView, part.vertexCount()));

			int normalView = bufferViews.size();
			int normalOffset = binary.size();
			writeNormals(binary, part);
			bufferViews.add(bufferView(normalOffset, part.vertexCount() * 3 * Float.BYTES, 34962));
			int normalAccessor = accessors.size();
			accessors.add(vec3Accessor(normalView, part.vertexCount()));

			int indexView = bufferViews.size();
			int indexOffset = binary.size();
			int indexCount = writeIndices(binary, part);
			bufferViews.add(bufferView(indexOffset, indexCount * Integer.BYTES, 34963));
			int indexAccessor = accessors.size();
			accessors.add(indexAccessor(indexView, indexCount));

			JsonObject attributes = new JsonObject();
			attributes.addProperty("POSITION", positionAccessor);
			attributes.addProperty("NORMAL", normalAccessor);

			JsonObject primitive = new JsonObject();
			primitive.add("attributes", attributes);
			primitive.addProperty("indices", indexAccessor);

			JsonObject mesh = new JsonObject();
			JsonArray primitives = new JsonArray();
			primitives.add(primitive);
			mesh.add("primitives", primitives);
			meshes.add(mesh);

			JsonObject node = new JsonObject();
			node.addProperty("name", entry.getKey());
			node.addProperty("mesh", meshes.size() - 1);
			nodes.add(node);
			sceneNodes.add(nodes.size() - 1);
		}

		JsonObject root = new JsonObject();
		JsonObject asset = new JsonObject();
		asset.addProperty("version", "2.0");
		asset.addProperty("generator", GENERATOR);
		root.add("asset", asset);

		JsonObject scene = new JsonObject();
		scene.add("nodes", sceneNodes);
		root.add("scenes", jsonArray(scene));
		root.addProperty("scene", 0);
		root.add("nodes", toJsonArray(nodes));
		root.add("meshes", toJsonArray(meshes));
		root.add("accessors", toJsonArray(accessors));
		root.add("bufferViews", toJsonArray(bufferViews));

		JsonObject buffer = new JsonObject();
		buffer.addProperty("byteLength", binary.size());
		buffer.addProperty("uri", "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(binary.toByteArray()));
		root.add("buffers", jsonArray(buffer));

		return root.toString();
	}

	public static String export(GeometryRecipe recipe) {
		GeometryResult geometry = GeometryRecipeExecutor.execute(recipe);
		return export(new MeshAssembler().assemble(geometry));
	}

	private static void writePositions(ByteArrayOutputStream binary, Mesh mesh, float unitScale) {
		float[] vertices = mesh.vertices();
		ByteBuffer buffer = ByteBuffer.allocate(mesh.vertexCount() * 3 * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
			int offset = vertex * Mesh.FLOATS_PER_VERTEX;
			buffer.putFloat(vertices[offset] * unitScale);
			buffer.putFloat(vertices[offset + 1] * unitScale);
			buffer.putFloat(vertices[offset + 2] * unitScale);
		}
		writeBuffer(binary, buffer.array());
	}

	private static void writeNormals(ByteArrayOutputStream binary, Mesh mesh) {
		float[] vertices = mesh.vertices();
		ByteBuffer buffer = ByteBuffer.allocate(mesh.vertexCount() * 3 * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
			int offset = vertex * Mesh.FLOATS_PER_VERTEX + 3;
			buffer.putFloat(vertices[offset]);
			buffer.putFloat(vertices[offset + 1]);
			buffer.putFloat(vertices[offset + 2]);
		}
		writeBuffer(binary, buffer.array());
	}

	private static int writeIndices(ByteArrayOutputStream binary, Mesh mesh) {
		int[] indices = mesh.indices();
		ByteBuffer buffer = ByteBuffer.allocate(indices.length * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (int index : indices) {
			buffer.putInt(index);
		}
		writeBuffer(binary, buffer.array());
		return indices.length;
	}

	private static void writeBuffer(ByteArrayOutputStream binary, byte[] bytes) {
		try {
			binary.write(bytes);
		} catch (IOException exception) {
			throw new IllegalStateException("failed to write glTF binary buffer", exception);
		}
	}

	private static JsonObject bufferView(int byteOffset, int byteLength, int target) {
		JsonObject view = new JsonObject();
		view.addProperty("buffer", 0);
		view.addProperty("byteOffset", byteOffset);
		view.addProperty("byteLength", byteLength);
		view.addProperty("target", target);
		return view;
	}

	private static JsonObject vec3Accessor(int bufferView, int count) {
		JsonObject accessor = new JsonObject();
		accessor.addProperty("bufferView", bufferView);
		accessor.addProperty("componentType", 5126);
		accessor.addProperty("count", count);
		accessor.addProperty("type", "VEC3");
		return accessor;
	}

	private static JsonObject indexAccessor(int bufferView, int count) {
		JsonObject accessor = new JsonObject();
		accessor.addProperty("bufferView", bufferView);
		accessor.addProperty("componentType", 5125);
		accessor.addProperty("count", count);
		accessor.addProperty("type", "SCALAR");
		return accessor;
	}

	private static JsonArray jsonArray(JsonObject object) {
		JsonArray array = new JsonArray();
		array.add(object);
		return array;
	}

	private static JsonArray toJsonArray(List<JsonObject> objects) {
		JsonArray array = new JsonArray();
		for (JsonObject object : objects) {
			array.add(object);
		}
		return array;
	}
}
