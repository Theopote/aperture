package dev.aperture.editor.plugin;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;

import java.util.List;

/** Family-owned editor semantics discovered independently of the host frontend. */
public interface ArchitecturalEditorPlugin {
	String id();
	boolean supports(ObjectEditorView view);
	List<ManipulatorDescriptor> manipulators(ObjectEditorView view, InspectorModel inspector);
}
