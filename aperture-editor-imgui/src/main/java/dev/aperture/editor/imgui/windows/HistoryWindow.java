package dev.aperture.editor.imgui.windows;
import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.editor.model.session.EditorSession;
import java.util.*;
public final class HistoryWindow {
	private final EditorSession session; public HistoryWindow(EditorSession session){this.session=Objects.requireNonNull(session);}
	public List<EditorHistoryEntry> design(){return session.history().designCommands();} public List<EditorHistoryEntry> runtime(){return session.history().runtimeEvents();} public List<EditorHistoryEntry> rejected(){return session.history().rejectedCommands();}
}
