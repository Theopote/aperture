package dev.aperture.editor.model.command;
import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.editor.model.read.*;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import java.time.Instant;
import java.util.*;
public final class DefaultEditorCommandGateway implements EditorCommandGateway {
	private final EditorCommandTransport transport; private final DiagnosticsModel diagnostics; private final RuntimeActionResolver runtimeActions;
	public DefaultEditorCommandGateway(EditorCommandTransport transport, DiagnosticsModel diagnostics){this(transport,diagnostics,new StandardRuntimeActionResolver());}
	public DefaultEditorCommandGateway(EditorCommandTransport transport, DiagnosticsModel diagnostics,RuntimeActionResolver runtimeActions){this.transport=Objects.requireNonNull(transport);this.diagnostics=Objects.requireNonNull(diagnostics);this.runtimeActions=Objects.requireNonNull(runtimeActions);}
	public EditorCommandSubmission submit(ArchitecturalCommand command){return submit(command,new ExpectedRevision(0,0));}
	public EditorCommandSubmission submit(ArchitecturalCommand command,ExpectedRevision revision){
		var result=transport.submit(UUID.randomUUID(),Objects.requireNonNull(command),Objects.requireNonNull(revision));
		if(result.status()==EditorCommandSubmission.Status.REJECTED||result.status()==EditorCommandSubmission.Status.REVISION_CONFLICT)
			diagnostics.add(new EditorDiagnostic(EditorDiagnostic.Severity.ERROR,result.status().name().toLowerCase(Locale.ROOT),result.message(),Optional.of(command.target().objectId()),Optional.empty(),"command",Instant.now(),result.status()==EditorCommandSubmission.Status.REVISION_CONFLICT?"Resync object":"Review command values",false));
		return result;
	}
	public EditorCommandSubmission submitRuntimeAction(dev.aperture.runtime.model.object.ArchitecturalObjectId objectId,String actionId,ExpectedRevision revision){return submit(runtimeActions.resolve(objectId,actionId),revision);}
	public EditorCommandSubmission undo(EditorHistoryEntry entry){if(!entry.undoable()||entry.compensation()==null) return rejected("History entry is not undoable",entry);return submit(entry.compensation().get(),entry.expectedRevision());}
	public EditorCommandSubmission redo(EditorHistoryEntry entry){if(entry.repetition()==null)return rejected("History entry is not redoable",entry);return submit(entry.repetition().get(),entry.expectedRevision());}
	private EditorCommandSubmission rejected(String message,EditorHistoryEntry e){return new EditorCommandSubmission(UUID.randomUUID(),EditorCommandSubmission.Status.REJECTED,message,e.revision(),e.expectedRevision().stateRevision());}
}
