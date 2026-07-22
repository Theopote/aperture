package dev.aperture.editor.model.history;

import dev.aperture.editor.model.command.*;
import dev.aperture.editor.model.read.DiagnosticsModel;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AsyncHistoryGatewayTest {
	@Test void serverRejectionRestoresCursorAndConflictAlsoLeavesEntryApplied() {
		var transport=new ControllableTransport();var history=new DefaultHistoryProjection();var entry=entry();history.recordDesign(entry);
		var gateway=new DefaultEditorCommandGateway(transport,new DiagnosticsModel(),new StandardRuntimeActionResolver(),null,history);
		var pending=gateway.undo(history);assertTrue(history.operationPending());
		transport.complete(pending.commandId(),EditorCommandSubmission.Status.REJECTED);assertEquals(1,history.cursor());assertFalse(history.operationPending());
		var conflict=gateway.undo(history);transport.complete(conflict.commandId(),EditorCommandSubmission.Status.REVISION_CONFLICT);assertEquals(1,history.cursor());
	}

	@Test void acceptedUndoThenRedoMovesCursorOnlyOnAuthorityResponse() {
		var transport=new ControllableTransport();var history=new DefaultHistoryProjection();history.recordDesign(entry());
		var gateway=new DefaultEditorCommandGateway(transport,new DiagnosticsModel(),new StandardRuntimeActionResolver(),null,history);
		var undo=gateway.undo(history);assertEquals(1,history.cursor());transport.complete(undo.commandId(),EditorCommandSubmission.Status.ACCEPTED);assertEquals(0,history.cursor());
		var redo=gateway.redo(history);assertEquals(0,history.cursor());transport.complete(redo.commandId(),EditorCommandSubmission.Status.ACCEPTED);assertEquals(1,history.cursor());
	}

	private static EditorHistoryEntry entry(){var id=ArchitecturalObjectId.random();return new EditorHistoryEntry(UUID.randomUUID(),id,"set_parameter","Width","900","1000","test",Instant.EPOCH,EditorHistoryEntry.Result.ACCEPTED,4,true,()->new SetParameterArchitecturalCommand(id,"width",ParameterValue.length(900)),()->new SetParameterArchitecturalCommand(id,"width",ParameterValue.length(1000)),new ExpectedRevision(4,0));}
	private static final class ControllableTransport implements EditorCommandTransport {
		private EditorCommandResultListener listener;
		public EditorCommandSubmission submit(UUID id,ArchitecturalCommand command,ExpectedRevision revision){return new EditorCommandSubmission(id,EditorCommandSubmission.Status.PENDING,"pending",revision.objectRevision(),revision.stateRevision());}
		public void addResultListener(EditorCommandResultListener listener){this.listener=listener;}
		void complete(UUID id,EditorCommandSubmission.Status status){listener.completed(new EditorCommandSubmission(id,status,status.name(),5,0));}
	}
}
