package dev.aperture.editor.interaction;

import dev.aperture.editor.model.read.ObjectEditorView;

import java.util.List;

/** Family/editor projection extension point for declaring object manipulators. */
@FunctionalInterface
public interface ManipulatorDescriptorProvider {
	List<ManipulatorDescriptor> descriptors(ObjectEditorView view);
}
