package dev.aperture.runtime.material;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.render.material.BlendMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads material catalog JSON from data packs.
 */
public final class MaterialCatalogReader {
	public MaterialCatalogEntry read(InputStream stream) throws IOException {
		JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		return parseEntry(root);
	}

	public MaterialCatalogEntry read(Path path) throws IOException {
		try (InputStream stream = Files.newInputStream(path)) {
			return read(stream);
		}
	}

	public Map<String, String> readSlotBindings(InputStream stream) throws IOException {
		JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		Map<String, String> bindings = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			bindings.put(entry.getKey(), entry.getValue().getAsString());
		}
		return bindings;
	}

	public Map<String, String> readSlotBindings(Path path) throws IOException {
		try (InputStream stream = Files.newInputStream(path)) {
			return readSlotBindings(stream);
		}
	}

	private static MaterialCatalogEntry parseEntry(JsonObject root) {
		return new MaterialCatalogEntry(
			root.get("id").getAsString(),
			root.get("albedoTexture").getAsString(),
			root.has("roughness") ? root.get("roughness").getAsFloat() : 0.5f,
			root.has("metalness") ? root.get("metalness").getAsFloat() : 0.0f,
			parseBlendMode(root.get("blendMode").getAsString()),
			root.has("doubleSided") && root.get("doubleSided").getAsBoolean()
		);
	}

	private static BlendMode parseBlendMode(String raw) {
		return switch (raw.toLowerCase()) {
			case "opaque" -> BlendMode.OPAQUE;
			case "cutout" -> BlendMode.CUTOUT;
			case "translucent" -> BlendMode.TRANSLUCENT;
			default -> throw new IllegalArgumentException("Unknown blend mode: " + raw);
		};
	}
}
