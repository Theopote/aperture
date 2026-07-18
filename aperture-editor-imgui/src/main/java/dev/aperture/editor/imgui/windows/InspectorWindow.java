package dev.aperture.editor.imgui.windows;
import dev.aperture.editor.model.inspector.InspectorSection;
import dev.aperture.editor.model.session.EditorSession;
import java.util.*;
public final class InspectorWindow {
	private final EditorSession session; public InspectorWindow(EditorSession session){this.session=Objects.requireNonNull(session);}
	public View view(){var selection=session.selection().snapshot();if(selection.objectIds().isEmpty())return new View("No architectural object selected",List.of(),false);if(selection.objectIds().size()>1)return new View(selection.objectIds().size()+" objects selected",List.of(),true);var id=selection.primaryObject();return new View("",session.inspector().sections(id),false);}
	public record View(String message,List<InspectorSection> sections,boolean multiple){public View{sections=List.copyOf(sections);}}
}
