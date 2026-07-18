package dev.aperture.editor.model.command;
import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
public interface EditorCommandGateway {
	EditorCommandSubmission submit(ArchitecturalCommand command);
	EditorCommandSubmission submit(ArchitecturalCommand command, ExpectedRevision expectedRevision);
	EditorCommandSubmission undo(EditorHistoryEntry entry);
	EditorCommandSubmission redo(EditorHistoryEntry entry);
}
