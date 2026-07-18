package dev.aperture.editor.model.command;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.Objects;
public record SetParameterArchitecturalCommand(ObjectRef target,String parameterKey,ParameterValue value) implements ArchitecturalCommand {
	public SetParameterArchitecturalCommand(ArchitecturalObjectId id,String key,ParameterValue value){this(new ObjectRef(id),key,value);}
	public SetParameterArchitecturalCommand{Objects.requireNonNull(target);Objects.requireNonNull(parameterKey);Objects.requireNonNull(value);}
	public String commandType(){return "aperture:set_parameter";}
}
