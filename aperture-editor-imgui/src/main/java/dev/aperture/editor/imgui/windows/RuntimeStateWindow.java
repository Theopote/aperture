package dev.aperture.editor.imgui.windows;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.runtime.model.state.StateValue;
import java.util.*;
public final class RuntimeStateWindow {
	private final EditorSession session; public RuntimeStateWindow(EditorSession session){this.session=Objects.requireNonNull(session);}
	public Map<String,StateValue> values(){var id=session.selection().snapshot().primaryObject();return id==null?Map.of():session.readModel().object(id).map(v->v.runtimeState().values()).orElse(Map.of());}
}
