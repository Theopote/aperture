package dev.aperture.editor.imgui.windows;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.editor.model.session.EditorSession;
import java.util.*;
public final class DiagnosticsWindow {
	private final EditorSession session; public DiagnosticsWindow(EditorSession session){this.session=Objects.requireNonNull(session);}
	public List<EditorDiagnostic> entries(Set<EditorDiagnostic.Severity> severities,boolean selectedOnly){var selected=session.selection().snapshot().objectIds();return session.diagnostics().all().stream().filter(d->severities.isEmpty()||severities.contains(d.severity())).filter(d->!selectedOnly||d.objectId().map(selected::contains).orElse(false)).toList();}
	public void clearResolved(){session.diagnostics().clearResolved();}
}
