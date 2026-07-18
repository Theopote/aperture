package dev.aperture.editor.model.session;
import dev.aperture.editor.model.command.EditorCommandGateway;
import dev.aperture.editor.model.history.HistoryProjection;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.editor.model.read.*;
import dev.aperture.editor.model.selection.SelectionModel;
public interface EditorSession extends AutoCloseable {
	SelectionModel selection(); EditorReadModel readModel(); EditorCommandGateway commands(); InspectorModel inspector(); PreviewCoordinator preview();
	HistoryProjection history(); DiagnosticsModel diagnostics(); WorkspaceModel workspace(); ToolController tools();
	default void close() {}
}
