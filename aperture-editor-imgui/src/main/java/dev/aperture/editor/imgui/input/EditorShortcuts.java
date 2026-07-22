package dev.aperture.editor.imgui.input;

import dev.aperture.editor.model.session.EditorSession;
import java.util.Objects;

public final class EditorShortcuts {
	private EditorShortcuts() {}
	public static ShortcutDispatcher create(EditorSession session,Runnable deleteSelection,Runnable focusSelection,Runnable selectAllVisible){
		Objects.requireNonNull(session);var dispatcher=new ShortcutDispatcher();
		dispatcher.bind("ESCAPE",()->{session.preview();session.tools().cancelActiveTool();});
		dispatcher.bind("DELETE",Objects.requireNonNull(deleteSelection));dispatcher.bind("F",Objects.requireNonNull(focusSelection));dispatcher.bind("CTRL+A",Objects.requireNonNull(selectAllVisible));
		dispatcher.bind("CTRL+Z",()->session.commands().undo(session.history()));
		dispatcher.bind("CTRL+SHIFT+Z",()->session.commands().redo(session.history()));dispatcher.bind("CTRL+Y",()->dispatcher.dispatch("CTRL+SHIFT+Z",new EditorInputPolicy(false,false,false)));
		return dispatcher;
	}
}
