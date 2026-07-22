package dev.aperture.editor.model.history;
import java.util.*;
/** Cursor-based authoritative history. Cursor moves only after the server accepts compensation/repetition. */
public final class DefaultHistoryProjection implements HistoryProjection {
	private final List<EditorHistoryEntry> design=new ArrayList<>(),runtime=new ArrayList<>(),rejected=new ArrayList<>();
	private int cursor; private PendingOperation pending;
	public synchronized void recordDesign(EditorHistoryEntry entry){if(entry.result()==EditorHistoryEntry.Result.REJECTED){recordRejected(entry);return;}if(cursor<design.size())design.subList(cursor,design.size()).clear();design.add(entry);cursor=design.size();}
	public synchronized void recordRuntime(EditorHistoryEntry entry){runtime.add(entry);}
	public synchronized void recordRejected(EditorHistoryEntry entry){rejected.add(entry);}
	public synchronized Optional<EditorHistoryEntry> beginUndo(UUID operationId){if(pending!=null||cursor==0)return Optional.empty();var entry=design.get(cursor-1);if(!entry.undoable()||entry.compensation()==null)return Optional.empty();pending=new PendingOperation(operationId,Direction.UNDO);return Optional.of(entry);}
	public synchronized Optional<EditorHistoryEntry> beginRedo(UUID operationId){if(pending!=null||cursor>=design.size())return Optional.empty();var entry=design.get(cursor);if(entry.repetition()==null)return Optional.empty();pending=new PendingOperation(operationId,Direction.REDO);return Optional.of(entry);}
	public synchronized void completeOperation(UUID operationId,boolean accepted){if(pending==null||!pending.id().equals(operationId))return;if(accepted)cursor+=pending.direction()==Direction.UNDO?-1:1;pending=null;}
	public synchronized boolean operationPending(){return pending!=null;} public synchronized int cursor(){return cursor;}
	public synchronized List<EditorHistoryEntry> designCommands(){return List.copyOf(design);}public synchronized List<EditorHistoryEntry> runtimeEvents(){return List.copyOf(runtime);}public synchronized List<EditorHistoryEntry> rejectedCommands(){return List.copyOf(rejected);}
	private enum Direction{UNDO,REDO} private record PendingOperation(UUID id,Direction direction){}
}