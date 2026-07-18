package dev.aperture.editor.model.inspector;
import dev.aperture.editor.model.read.EditorReadModel;
import dev.aperture.parameter.*;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public final class SchemaDrivenInspectorModel implements InspectorModel {
	private final EditorReadModel readModel; public SchemaDrivenInspectorModel(EditorReadModel readModel){this.readModel=Objects.requireNonNull(readModel);}
	public List<InspectorSection> sections(ArchitecturalObjectId id){return readModel.object(id).map(v->{
		var identity=List.of(new InspectorProperty("type","Type ID",InspectorProperty.Control.TEXT,v.typeId().toString(),"",true),new InspectorProperty("family","Family ID",InspectorProperty.Control.TEXT,v.familyId().toString(),"",true));
		var geometry=v.parameters().asMap().entrySet().stream().map(e->new InspectorProperty(e.getKey(),e.getKey(),control(e.getValue()),e.getValue(),unit(e.getValue()),false)).toList();
		var runtime=v.runtimeState().values().entrySet().stream().map(e->new InspectorProperty(e.getKey(),e.getKey(),InspectorProperty.Control.TEXT,e.getValue(),"",true)).toList();
		return List.of(new InspectorSection("identity","Identity",identity),new InspectorSection("geometry","Geometry",geometry),new InspectorSection("runtime","Runtime State",runtime));
	}).orElse(List.of());}
	private static InspectorProperty.Control control(ParameterValue v){return switch(v.type()){case BOOL->InspectorProperty.Control.BOOLEAN;case ENUM->InspectorProperty.Control.ENUM;case MATERIAL_REF->InspectorProperty.Control.MATERIAL;default->InspectorProperty.Control.NUMBER;};}
	private static String unit(ParameterValue v){return switch(v.type()){case LENGTH->"mm";case ANGLE->"°";default->"";};}
}
