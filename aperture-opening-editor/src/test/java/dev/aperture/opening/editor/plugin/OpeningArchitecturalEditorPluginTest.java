package dev.aperture.opening.editor.plugin;

import dev.aperture.editor.plugin.ArchitecturalEditorPluginRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpeningArchitecturalEditorPluginTest {
	@Test
	void openingEditorPluginIsDiscoveredThroughServiceLoader() {
		var registry = ArchitecturalEditorPluginRegistry.discover();
		assertTrue(registry.plugins().stream().anyMatch(plugin -> plugin instanceof OpeningArchitecturalEditorPlugin));
	}
}
