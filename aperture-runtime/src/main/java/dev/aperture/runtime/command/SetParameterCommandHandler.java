package dev.aperture.runtime.command;

import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.ParameterPatch;
import dev.aperture.runtime.model.command.SetParameterCommand;

import java.util.List;

/** Family-neutral validation for edits to an existing typed parameter override. */
public final class SetParameterCommandHandler implements CommandHandler<SetParameterCommand> {
	@Override public String commandType() { return "aperture:set_parameter"; }
	@Override public Class<SetParameterCommand> commandClass() { return SetParameterCommand.class; }

	@Override
	public CommandResult handle(CommandEnvelope<SetParameterCommand> envelope, CommandContext context) {
		SetParameterCommand command = envelope.command();
		ParameterValue current = context.instance().parameterOverrides().get(command.parameter()).orElse(null);
		if (current == null) return CommandResult.rejected("parameter.unknown", "Unknown parameter: " + command.parameter());
		if (current.type() != command.value().type()) return CommandResult.rejected("parameter.type_mismatch",
			"Expected " + current.type() + " for " + command.parameter() + " but got " + command.value().type());
		return CommandResult.acceptedParameters(List.of(
			new ParameterPatch(envelope.expectedObjectRevision(), command.parameter(), command.value())));
	}
}
