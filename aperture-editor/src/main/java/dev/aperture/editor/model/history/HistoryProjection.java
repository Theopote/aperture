package dev.aperture.editor.model.history;
import java.util.List;
public interface HistoryProjection { List<EditorHistoryEntry> designCommands(); List<EditorHistoryEntry> runtimeEvents(); List<EditorHistoryEntry> rejectedCommands(); }
