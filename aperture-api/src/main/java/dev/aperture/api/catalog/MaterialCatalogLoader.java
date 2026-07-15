package dev.aperture.api.catalog;

import dev.aperture.api.material.MaterialCatalogEntry;
import dev.aperture.api.material.MaterialCatalogReader;
import dev.aperture.render.material.MaterialDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads material catalog entries and slot bindings from data pack resources.
 */
public final class MaterialCatalogLoader {
	private final MaterialCatalogReader reader = new MaterialCatalogReader();

	public List<MaterialCatalogEntry> loadClasspathDirectory(String resourceDirectory) {
		List<MaterialCatalogEntry> entries = new ArrayList<>();
		for (String resource : List.of(
			resourceDirectory + "/frame_oak.json",
			resourceDirectory + "/glazing_clear.json",
			resourceDirectory + "/hardware_iron.json"
		)) {
			entries.add(loadClasspathResource(resource));
		}
		return entries;
	}

	public MaterialCatalogEntry loadClasspathResource(String resourcePath) {
		InputStream stream = MaterialCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing classpath resource: " + resourcePath);
		}
		try (stream) {
			return reader.read(stream);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read material resource: " + resourcePath, exception);
		}
	}

	public Map<String, String> loadClasspathSlotBindings(String resourcePath) {
		InputStream stream = MaterialCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing classpath resource: " + resourcePath);
		}
		try (stream) {
			return reader.readSlotBindings(stream);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read slot bindings: " + resourcePath, exception);
		}
	}

	public MaterialCatalogRegistry loadClasspathCatalog() {
		MaterialCatalogRegistry registry = new MaterialCatalogRegistry();
		for (MaterialCatalogEntry entry : loadClasspathDirectory("aperture/materials")) {
			registry.register(entry.toDefinition());
		}
		for (var slotBinding : loadClasspathSlotBindings("aperture/material_slots.json").entrySet()) {
			registry.setSlotDefault(slotBinding.getKey(), slotBinding.getValue());
		}
		return registry;
	}

	public List<MaterialCatalogEntry> loadDirectory(Path directory) throws IOException {
		List<MaterialCatalogEntry> entries = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.filter(path -> path.toString().endsWith(".json"))
				.filter(path -> !path.getFileName().toString().equals("material_slots.json"))
				.forEach(path -> {
					try {
						entries.add(reader.read(path));
					} catch (IOException exception) {
						throw new IllegalStateException("Failed to read material: " + path, exception);
					}
				});
		}
		return entries;
	}
}
