package dev.aperture.core.catalog;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.serialization.OpeningTypeDefinitionReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads opening type definitions from a directory or classpath resource root.
 */
public final class OpeningTypeCatalogLoader {
	private final OpeningTypeDefinitionReader reader = new OpeningTypeDefinitionReader();

	public List<OpeningTypeDefinition> loadDirectory(Path directory) throws IOException {
		List<OpeningTypeDefinition> definitions = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.filter(path -> path.toString().endsWith(".json"))
				.forEach(path -> {
					try {
						definitions.add(reader.read(path));
					} catch (IOException exception) {
						throw new IllegalStateException("Failed to read opening type: " + path, exception);
					}
				});
		}
		return definitions;
	}

	public OpeningTypeDefinition loadClasspathResource(String resourcePath) {
		InputStream stream = OpeningTypeCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing classpath resource: " + resourcePath);
		}
		try (stream) {
			return reader.read(stream);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read opening type resource: " + resourcePath, exception);
		}
	}

	public void registerAll(OpeningTypeRegistry registry, Iterable<OpeningTypeDefinition> definitions) {
		for (OpeningTypeDefinition definition : definitions) {
			registry.register(definition);
		}
	}
}
