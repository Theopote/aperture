package dev.aperture.editor.imgui.input;

import dev.aperture.editor.model.session.EditorSession;
import java.util.Objects;

public final class EditorShortcuts {
	private EditorShortcuts() {}
	public static ShortcutDispatcher create(EditorSession session,Runnable deleteSelection,Runnable focusSelection,Runnable selectAllVisible){
		Objects.requireNonNull(session);var dispatcher=new ShortcutDispatcher();
		dispatcher.bind("ESCAPE",()->{session.preview();session.tools().cancelActiveTool();});
		dispatcher.bind("DELETE",Objects.requireNonNull(deleteSelection));dispatcher.bind("F",Objects.requireNonNull(focusSelection));dispatcher.bind("CTRL+A",Objects.requireNonNull(selectAllVisible));
		dispatcher.bind("CTRL+Z",()->session.history().designCommands().stream().filter(e->e.undoable()&&e.result()==dev.aperture.editor.model.history.EditorHistoryEntry.Result.ACCEPTED).reduce((a,b)->b).ifPresent(session.commands()::undo));
		dispatcher.bind("CTRL+SHIFT+Z",()->session.history().designCommands().stream().reduce((a,b)->b).ifPresent(session.commands()::redo));dispatcher.bind("CTRL+Y",()->dispatcher.dispatch("CTRL+SHIFT+Z",new EditorInputPolicy(false,false,false)));
		return dispatcher;
	}
}
