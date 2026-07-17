package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.capability.OpenableCapability;
import dev.aperture.runtime.model.capability.StandardCapabilities;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;

import java.util.List;
import java.util.Map;

public final class RequestOpenDoorHandler implements CommandHandler<RequestOpenCommand> {
	@Override public String commandType() { return "aperture:request_open"; }
	@Override public Class<RequestOpenCommand> commandClass() { return RequestOpenCommand.class; }

	@Override
	public CommandResult handle(CommandEnvelope<RequestOpenCommand> envelope, CommandContext context) {
		OpenableCapability openable = context.capabilities().requireCapability(StandardCapabilities.OPENABLE);
		if (!openable.canRequestOpen()) return CommandResult.rejected("door.open_denied", "Door cannot be opened");
		return CommandResult.accepted(List.of(new StatePatch(context.state().revision(), Map.of(
			DoorStateSchema.TARGET_OPEN_RATIO, StateValue.number(1),
			DoorStateSchema.MOTION, StateValue.enumeration("opening"),
			DoorStateSchema.LAST_INTERACTOR, StateValue.string(envelope.actor().id())
		), envelope.timestamp())));
	}
}
