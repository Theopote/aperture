package dev.aperture.editor.imgui;

import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.preview.ParameterEditSession;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.ImGui;
import imgui.type.ImBoolean;

import java.util.Objects;

/** Bridges continuous ImGui controls to one local preview session and one final command. */
final class DearImGuiPropertyEditor {
	private final EditorSession session;
	private ParameterEditSession active;
	DearImGuiPropertyEditor(EditorSession session){this.session=Objects.requireNonNull(session);}

	boolean render(ArchitecturalObjectId objectId, PropertyDescriptor property, ParameterValue value) {
		if (property.readOnly()) return false;
		boolean changed; ParameterValue next; String widgetId="##parameter-"+property.key();
		if(value instanceof ParameterValue.BoolValue bool){ImBoolean data=new ImBoolean(bool.value());changed=ImGui.checkbox(widgetId,data);next=ParameterValue.bool(data.get());}
		else if(value.type()==ParameterType.LENGTH||value.type()==ParameterType.ANGLE||value.type()==ParameterType.NUMBER){
			float[] data={(float)value.asNumber()};float speed=(float)property.step().orElse(0.1);float min=(float)property.minimum().orElse(0);float max=(float)property.maximum().orElse(0);changed=ImGui.dragFloat(widgetId,data,speed,min,max,"%."+property.precision()+"f");
			next=switch(value.type()){case LENGTH->ParameterValue.length(data[0]);case ANGLE->ParameterValue.angle(data[0]);default->ParameterValue.number(data[0]);};
		}else return false;
		if(ImGui.isItemActivated())begin(objectId,property.key(),value);
		if(changed&&active!=null&&active.active())active.updatePreview(next);
		if(ImGui.isItemDeactivatedAfterEdit()&&active!=null&&active.active()){active.commit();active=null;}
		return true;
	}

	void cancel(){if(active!=null&&active.active())active.cancel();active=null;}

	private void begin(ArchitecturalObjectId id,String key,ParameterValue authoritative){
		cancel();var view=session.readModel().object(id).orElseThrow();
		active=new DefaultParameterEditSession(id,key,authoritative,new ExpectedRevision(view.objectRevision(),view.stateRevision()),session.preview(),session.commands());
	}
}
