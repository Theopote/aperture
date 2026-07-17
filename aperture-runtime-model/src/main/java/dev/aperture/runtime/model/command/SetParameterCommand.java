package dev.aperture.runtime.model.command;

import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record SetParameterCommand(ObjectRef target, String parameter, ParameterValue value) implements ArchitecturalCommand {
	public SetParameterCommand {
		Objects.requireNonNull(target, "target"); Objects.requireNonNull(parameter, "parameter"); Objects.requireNonNull(value, "value");
		if (parameter.isBlank()) throw new IllegalArgumentException("parameter must not be blank");
	}
	@Override public String commandType() { return "aperture:set_parameter"; }
}
