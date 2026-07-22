package dev.aperture.editor.model.history;
import java.util.*;
public interface HistoryProjection {
	List<EditorHistoryEntry> designCommands(); List<EditorHistoryEntry> runtimeEvents(); List<EditorHistoryEntry> rejectedCommands();
	void recordDesign(EditorHistoryEntry entry); void recordRuntime(EditorHistoryEntry entry); void recordRejected(EditorHistoryEntry entry);
	Optional<EditorHistoryEntry> beginUndo(UUID operationId); Optional<EditorHistoryEntry> beginRedo(UUID operationId);
	void completeOperation(UUID operationId, boolean accepted); boolean operationPending(); int cursor();
}