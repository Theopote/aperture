package dev.aperture.editor.model.preview;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public final class LocalPreviewCoordinator implements PreviewCoordinator {
	private final Map<ArchitecturalObjectId,Map<String,ParameterValue>> values=new HashMap<>();
	private final Map<UUID,PendingPreview> pending=new HashMap<>();
	public synchronized void put(ArchitecturalObjectId id,String key,ParameterValue value){values.computeIfAbsent(id,x->new HashMap<>()).put(key,value);}
	public synchronized Optional<ParameterValue> value(ArchitecturalObjectId id,String key){return Optional.ofNullable(values.getOrDefault(id,Map.of()).get(key));}
	public synchronized Map<String,ParameterValue> values(ArchitecturalObjectId id){return Map.copyOf(values.getOrDefault(id,Map.of()));}
	public synchronized void associate(UUID commandId,ArchitecturalObjectId id,String key){pending.put(commandId,new PendingPreview(id,key,PreviewState.PENDING));}
	public synchronized void transition(UUID commandId,PreviewState state){pending.computeIfPresent(commandId,(id,current)->new PendingPreview(current.objectId(),current.parameterKey(),state));}
	public synchronized Optional<PreviewState> state(UUID commandId){return Optional.ofNullable(pending.get(commandId)).map(PendingPreview::state);}
	public synchronized void complete(UUID commandId){var preview=pending.get(commandId);if(preview!=null){var object=values.get(preview.objectId());if(object!=null){object.remove(preview.parameterKey());if(object.isEmpty())values.remove(preview.objectId());}}}
	public synchronized void dismiss(UUID commandId){pending.remove(commandId);}
	public synchronized void clear(ArchitecturalObjectId id,String key){var object=values.get(id);if(object!=null){object.remove(key);if(object.isEmpty())values.remove(id);}pending.entrySet().removeIf(entry->entry.getValue().objectId().equals(id)&&entry.getValue().parameterKey().equals(key));}
	public synchronized void clearObject(ArchitecturalObjectId id){values.remove(id);pending.entrySet().removeIf(entry->entry.getValue().objectId().equals(id));}
	private record PendingPreview(ArchitecturalObjectId objectId,String parameterKey,PreviewState state){}
}