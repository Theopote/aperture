package dev.aperture.geometry.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads profile catalog entries from data pack resources.
 */
public final class ProfileCatalogLoader {
	private final ProfileCatalogReader reader = new ProfileCatalogReader();

	public ProfileCatalogRegistry loadClasspathCatalog() {
		ProfileCatalogRegistry registry = new ProfileCatalogRegistry();
		for (ProfileDefinition definition : loadClasspathDirectory("aperture/profiles")) {
			registry.register(definition);
		}
		return registry;
	}

	public List<ProfileDefinition> loadClasspathDirectory(String resourceDirectory) {
		List<ProfileDefinition> entries = new ArrayList<>();
		for (String fileName : readClasspathIndex(resourceDirectory + "/index.json")) {
			entries.add(loadClasspathResource(resourceDirectory + "/" + fileName + ".json"));
		}
		return entries;
	}

	public ProfileDefinition loadClasspathResource(String resourcePath) {
		InputStream stream = ProfileCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing classpath resource: " + resourcePath);
		}
		try (stream) {
			return reader.read(stream);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read profile resource: " + resourcePath, exception);
		}
	}

	public List<ProfileDefinition> loadDirectory(Path directory) throws IOException {
		List<ProfileDefinition> entries = new ArrayList<>();
		Path indexPath = directory.resolve("index.json");
		List<String> fileNames;
		try (InputStream stream = Files.newInputStream(indexPath)) {
			fileNames = reader.readIndex(stream);
		}
		for (String fileName : fileNames) {
			entries.add(reader.read(directory.resolve(fileName + ".json")));
		}
		return entries;
	}

	private List<String> readClasspathIndex(String resourcePath) {
		InputStream stream = ProfileCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing classpath resource: " + resourcePath);
		}
		try (stream) {
			return reader.readIndex(stream);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read profile index: " + resourcePath, exception);
		}
	}
}
