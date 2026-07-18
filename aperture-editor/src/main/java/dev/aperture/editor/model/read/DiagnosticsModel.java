package dev.aperture.editor.model.read;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public final class DiagnosticsModel {
	private final List<EditorDiagnostic> entries=new ArrayList<>();
	public synchronized void add(EditorDiagnostic value){entries.add(Objects.requireNonNull(value));}
	public synchronized List<EditorDiagnostic> all(){return List.copyOf(entries);}
	public synchronized List<EditorDiagnostic> forObject(ArchitecturalObjectId id){return entries.stream().filter(d->d.objectId().filter(id::equals).isPresent()).toList();}
	public synchronized void clearResolved(){entries.removeIf(EditorDiagnostic::resolved);}
}
