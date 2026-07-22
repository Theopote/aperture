package dev.aperture.editor.plugin;

import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchitecturalEditorPluginRegistryTest {
	@Test
	void rejectsDuplicatePluginIds() {
		ArchitecturalEditorPlugin first = plugin("duplicate");
		ArchitecturalEditorPlugin second = plugin("duplicate");
		assertThrows(IllegalArgumentException.class,
			() -> new ArchitecturalEditorPluginRegistry(List.of(first, second)));
	}

	private static ArchitecturalEditorPlugin plugin(String id) {
		return new ArchitecturalEditorPlugin() {
			@Override public String id() { return id; }
			@Override public boolean supports(ObjectEditorView view) { return false; }
			@Override public List<dev.aperture.editor.interaction.ManipulatorDescriptor> manipulators(
				ObjectEditorView view, InspectorModel inspector) { return List.of(); }
		};
	}
}
