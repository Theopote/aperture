package dev.aperture.editor.model.inspector;

import dev.aperture.core.parametric.*;
import dev.aperture.editor.model.read.EditorReadModel;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;

import java.util.*;
import java.util.function.Function;

/** Builds Inspector descriptors from the registered parameter schema, never from widget defaults. */
public final class SchemaDrivenInspectorModel implements InspectorModel {
	private final EditorReadModel readModel;
	private final Function<ArchitecturalTypeId, Optional<ParametricSchema>> schemas;

	public SchemaDrivenInspectorModel(EditorReadModel readModel) { this(readModel, ignored -> Optional.empty()); }
	public SchemaDrivenInspectorModel(EditorReadModel readModel, Function<ArchitecturalTypeId, Optional<ParametricSchema>> schemas) {
		this.readModel=Objects.requireNonNull(readModel); this.schemas=Objects.requireNonNull(schemas);
	}

	public List<InspectorSection> sections(ArchitecturalObjectId id) { return readModel.object(id).map(view -> {
		var identity=List.of(PropertyDescriptor.readOnlyText("type","Type ID",view.typeId().toString(),"Identity"),
			PropertyDescriptor.readOnlyText("family","Family ID",view.familyId().toString(),"Identity"));
		var schema=schemas.apply(view.typeId());
		var geometry=schema.map(value -> value.parameters().entrySet().stream().map(entry -> descriptor(entry.getKey(),entry.getValue(),
			view.parameters().get(entry.getKey()).orElse(entry.getValue().defaultValue()))).toList())
			.orElseGet(() -> view.parameters().asMap().entrySet().stream().map(entry -> fallback(entry.getKey(),entry.getValue())).toList());
		var runtime=view.runtimeState().values().entrySet().stream().map(entry ->
			PropertyDescriptor.readOnlyText(entry.getKey(),entry.getKey(),entry.getValue(),"Runtime State")).toList();
		return List.of(new InspectorSection("identity","Identity",identity),new InspectorSection("geometry","Geometry",geometry),
			new InspectorSection("runtime","Runtime State",runtime));
	}).orElse(List.of()); }

	private static PropertyDescriptor descriptor(String key, Parameter parameter, ParameterValue value) {
		OptionalDouble min=OptionalDouble.empty(), max=OptionalDouble.empty(), step=OptionalDouble.empty();
		if(parameter instanceof NumberParameter number){min=number.min();max=number.max();step=number.step();}
		else if(parameter instanceof RangeParameter range){min=OptionalDouble.of(range.min());max=OptionalDouble.of(range.max());step=range.step();}
		List<String> options=parameter instanceof EnumParameter enumeration ? enumeration.values()
			: parameter instanceof ChoiceParameter choice ? choice.values() : List.of();
		String label=parameter.label().isBlank()?humanize(key):parameter.label();
		String group=parameter.group().isBlank()?"Geometry":parameter.group();
		var issues=new java.util.ArrayList<dev.aperture.core.validation.ValidationIssue>();parameter.validateValue(key,value,issues);
		return new PropertyDescriptor(key,label,parameter.metadata().description(),parameter.storageType(),value,parameter.defaultValue(),
			unit(parameter.storageType()),min,max,step,precision(step),options,parameter.metadata().readOnly(),true,group,issues.stream().map(dev.aperture.core.validation.ValidationIssue::message).toList(),widget(parameter));
	}

	private static PropertyDescriptor fallback(String key, ParameterValue value) {
		return new PropertyDescriptor(key,humanize(key),"Schema unavailable",value.type(),value,value,"",OptionalDouble.empty(),
			OptionalDouble.empty(),OptionalDouble.empty(),2,List.of(),true,true,"Unschematized",List.of("No parameter schema registered"),PropertyDescriptor.PreferredWidget.TEXT);
	}
	private static PropertyDescriptor.PreferredWidget widget(Parameter parameter){return switch(parameter.kind()){
		case RANGE->PropertyDescriptor.PreferredWidget.SLIDER; case NUMBER->PropertyDescriptor.PreferredWidget.DRAG;
		case BOOLEAN->PropertyDescriptor.PreferredWidget.CHECKBOX; case CHOICE,ENUM->PropertyDescriptor.PreferredWidget.COMBO;
		case MATERIAL->PropertyDescriptor.PreferredWidget.MATERIAL;};}
	private static String unit(ParameterType type){return switch(type){case LENGTH->"mm";case ANGLE->"°";default->"";};}
	private static int precision(OptionalDouble step){if(step.isEmpty())return 2;double value=step.getAsDouble();int precision=0;while(precision<6&&Math.abs(value-Math.rint(value))>1e-9){value*=10;precision++;}return precision;}
	private static String humanize(String key){if(key.isBlank())return key;String spaced=key.replace('_',' ');return Character.toUpperCase(spaced.charAt(0))+spaced.substring(1);}
}