package dev.aperture.geometry.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.math.Vec2d;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads profile catalog JSON from data packs.
 */
public final class ProfileCatalogReader {
	public ProfileDefinition read(InputStream stream) throws IOException {
		JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		return parseEntry(root);
	}

	public ProfileDefinition read(Path path) throws IOException {
		try (InputStream stream = Files.newInputStream(path)) {
			return read(stream);
		}
	}

	public List<String> readIndex(InputStream stream) throws IOException {
		JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		JsonArray profiles = root.getAsJsonArray("profiles");
		List<String> ids = new ArrayList<>();
		for (JsonElement element : profiles) {
			ids.add(element.getAsString());
		}
		return ids;
	}

	private static ProfileDefinition parseEntry(JsonObject root) {
		List<Vec2d> points = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("points")) {
			JsonObject point = element.getAsJsonObject();
			points.add(new Vec2d(point.get("u").getAsDouble(), point.get("v").getAsDouble()));
		}
		return new ProfileDefinition(
			root.get("id").getAsString(),
			root.get("name").getAsString(),
			ProfileCurve.fromPoints(points)
		);
	}
}
