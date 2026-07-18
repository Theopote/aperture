package dev.aperture.editor.imgui.workspace;
import java.util.*;
public final class MainDockspace {
	private final Map<String,EditorWindow> windows=new LinkedHashMap<>();
	public MainDockspace window(String id,EditorWindow window){windows.put(Objects.requireNonNull(id),Objects.requireNonNull(window));return this;}
	public void render(){windows.values().forEach(EditorWindow::render);}
	public Set<String> windowIds(){return Set.copyOf(windows.keySet());}
}
