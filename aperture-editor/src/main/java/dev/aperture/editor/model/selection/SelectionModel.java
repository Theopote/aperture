package dev.aperture.editor.model.selection;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

public interface SelectionModel {
	SelectionSnapshot snapshot();
	void selectObject(ArchitecturalObjectId objectId);
	void selectComponent(ArchitecturalObjectId objectId, ComponentPath componentPath);
	void addObject(ArchitecturalObjectId objectId);
	void removeObject(ArchitecturalObjectId objectId);
	void clear();
	void addListener(SelectionListener listener);
	void removeListener(SelectionListener listener);
	interface SelectionListener { void selectionChanged(SelectionSnapshot selection); }
}
