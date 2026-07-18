package dev.aperture.editor.model.selection;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;

public final class DefaultSelectionModel implements SelectionModel {
	private final LinkedHashSet<ArchitecturalObjectId> ids = new LinkedHashSet<>();
	private final List<SelectionListener> listeners = new ArrayList<>();
	private ArchitecturalObjectId primary;
	private ComponentSelection component;
	private long revision;
	public SelectionSnapshot snapshot() { return new SelectionSnapshot(ids, primary, Optional.ofNullable(component), revision); }
	public void selectObject(ArchitecturalObjectId id) { Objects.requireNonNull(id); ids.clear(); ids.add(id); primary=id; component=null; changed(); }
	public void selectComponent(ArchitecturalObjectId id, ComponentPath path) { selectObject(id); component=new ComponentSelection(id,path); changed(); }
	public void addObject(ArchitecturalObjectId id) { if(ids.add(Objects.requireNonNull(id))) { if(primary==null) primary=id; changed(); } }
	public void removeObject(ArchitecturalObjectId id) { if(ids.remove(id)) { if(Objects.equals(primary,id)) primary=ids.stream().findFirst().orElse(null); if(component!=null&&component.objectId().equals(id)) component=null; changed(); } }
	public void clear() { if(!ids.isEmpty()||component!=null) { ids.clear(); primary=null; component=null; changed(); } }
	public void addListener(SelectionListener listener) { listeners.add(Objects.requireNonNull(listener)); }
	public void removeListener(SelectionListener listener) { listeners.remove(listener); }
	private void changed(){ revision++; var value=snapshot(); List.copyOf(listeners).forEach(l->l.selectionChanged(value)); }
}
