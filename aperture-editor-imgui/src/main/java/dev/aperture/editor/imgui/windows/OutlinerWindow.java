package dev.aperture.editor.imgui.windows;
import dev.aperture.editor.model.read.ObjectSummary;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public final class OutlinerWindow {
	private final EditorSession session; public OutlinerWindow(EditorSession session){this.session=Objects.requireNonNull(session);}
	public List<ObjectSummary> rows(){return session.readModel().visibleObjects();}
	public void select(ArchitecturalObjectId id,boolean additive){if(additive)session.selection().addObject(id);else session.selection().selectObject(id);}
}
