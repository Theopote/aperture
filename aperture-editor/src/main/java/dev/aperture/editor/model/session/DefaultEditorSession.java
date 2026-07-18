package dev.aperture.editor.model.session;
import dev.aperture.editor.model.command.EditorCommandGateway;
import dev.aperture.editor.model.history.HistoryProjection;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.editor.model.read.*;
import dev.aperture.editor.model.selection.SelectionModel;
import java.util.Objects;
public final class DefaultEditorSession implements EditorSession {
	private final SelectionModel selection; private final EditorReadModel read; private final EditorCommandGateway commands; private final InspectorModel inspector; private final PreviewCoordinator preview; private final HistoryProjection history; private final DiagnosticsModel diagnostics; private final WorkspaceModel workspace; private final ToolController tools;
	public DefaultEditorSession(SelectionModel selection,EditorReadModel read,EditorCommandGateway commands,InspectorModel inspector,PreviewCoordinator preview,HistoryProjection history,DiagnosticsModel diagnostics,WorkspaceModel workspace,ToolController tools){this.selection=Objects.requireNonNull(selection);this.read=Objects.requireNonNull(read);this.commands=Objects.requireNonNull(commands);this.inspector=Objects.requireNonNull(inspector);this.preview=Objects.requireNonNull(preview);this.history=Objects.requireNonNull(history);this.diagnostics=Objects.requireNonNull(diagnostics);this.workspace=Objects.requireNonNull(workspace);this.tools=Objects.requireNonNull(tools);}
	public SelectionModel selection(){return selection;} public EditorReadModel readModel(){return read;} public EditorCommandGateway commands(){return commands;} public InspectorModel inspector(){return inspector;} public PreviewCoordinator preview(){return preview;} public HistoryProjection history(){return history;} public DiagnosticsModel diagnostics(){return diagnostics;} public WorkspaceModel workspace(){return workspace;} public ToolController tools(){return tools;}
}
