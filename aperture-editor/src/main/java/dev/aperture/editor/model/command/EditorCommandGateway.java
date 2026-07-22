package dev.aperture.editor.model.command;
import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
public interface EditorCommandGateway {
	EditorCommandSubmission submit(ArchitecturalCommand command);
	EditorCommandSubmission submit(ArchitecturalCommand command, ExpectedRevision expectedRevision);
	EditorCommandSubmission submitRuntimeAction(ArchitecturalObjectId objectId, String actionId, ExpectedRevision expectedRevision);
	default EditorCommandSubmission submitParameterEdit(ArchitecturalObjectId objectId,String key,dev.aperture.parameter.ParameterValue before,dev.aperture.parameter.ParameterValue after,ExpectedRevision revision){return submit(new SetParameterArchitecturalCommand(objectId,key,after),revision);}
	EditorCommandSubmission undo(EditorHistoryEntry entry);
	default EditorCommandSubmission undo(dev.aperture.editor.model.history.HistoryProjection history){throw new UnsupportedOperationException();}
	EditorCommandSubmission redo(EditorHistoryEntry entry);
	default EditorCommandSubmission redo(dev.aperture.editor.model.history.HistoryProjection history){throw new UnsupportedOperationException();}
}
