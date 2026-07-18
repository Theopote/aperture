package dev.aperture.editor.model.inspector;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.List;
public interface InspectorModel { List<InspectorSection> sections(ArchitecturalObjectId objectId); }
