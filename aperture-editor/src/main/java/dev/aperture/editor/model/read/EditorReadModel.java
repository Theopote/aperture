package dev.aperture.editor.model.read;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public interface EditorReadModel {
	Optional<ObjectEditorView> object(ArchitecturalObjectId objectId);
	List<ObjectSummary> visibleObjects();
	List<EditorDiagnostic> diagnostics(ArchitecturalObjectId objectId);
}
