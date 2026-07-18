package dev.aperture.editor.model.inspector;
import java.util.List;
public record InspectorSection(String id,String label,List<InspectorProperty> properties){public InspectorSection{properties=List.copyOf(properties);}}
