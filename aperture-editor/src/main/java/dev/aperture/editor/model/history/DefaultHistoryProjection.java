package dev.aperture.editor.model.history;
import java.util.*;
public final class DefaultHistoryProjection implements HistoryProjection {
	private final List<EditorHistoryEntry> design=new ArrayList<>(), runtime=new ArrayList<>(), rejected=new ArrayList<>();
	public synchronized void recordDesign(EditorHistoryEntry e){(e.result()==EditorHistoryEntry.Result.REJECTED?rejected:design).add(e);}
	public synchronized void recordRuntime(EditorHistoryEntry e){runtime.add(e);}
	public synchronized List<EditorHistoryEntry> designCommands(){return List.copyOf(design);}
	public synchronized List<EditorHistoryEntry> runtimeEvents(){return List.copyOf(runtime);}
	public synchronized List<EditorHistoryEntry> rejectedCommands(){return List.copyOf(rejected);}
}
