package dev.aperture.editor.imgui.input;
import java.util.*;
public final class ShortcutDispatcher {
	private final Map<String,Runnable> actions=new HashMap<>();
	public void bind(String chord,Runnable action){actions.put(Objects.requireNonNull(chord),Objects.requireNonNull(action));}
	public boolean dispatch(String chord,EditorInputPolicy policy){if(!policy.allowGlobalShortcut())return false;var action=actions.get(chord);if(action==null)return false;action.run();return true;}
}
