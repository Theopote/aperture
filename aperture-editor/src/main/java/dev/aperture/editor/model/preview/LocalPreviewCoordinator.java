package dev.aperture.editor.model.preview;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public final class LocalPreviewCoordinator implements PreviewCoordinator {
	private final Map<ArchitecturalObjectId,Map<String,ParameterValue>> values=new HashMap<>();
	public synchronized void put(ArchitecturalObjectId id,String key,ParameterValue value){values.computeIfAbsent(id,x->new HashMap<>()).put(key,value);}
	public synchronized Optional<ParameterValue> value(ArchitecturalObjectId id,String key){return Optional.ofNullable(values.getOrDefault(id,Map.of()).get(key));}
	public synchronized Map<String,ParameterValue> values(ArchitecturalObjectId id){return Map.copyOf(values.getOrDefault(id,Map.of()));}
	public synchronized void clear(ArchitecturalObjectId id,String key){var object=values.get(id);if(object!=null){object.remove(key);if(object.isEmpty())values.remove(id);}}
	public synchronized void clearObject(ArchitecturalObjectId id){values.remove(id);}
}
