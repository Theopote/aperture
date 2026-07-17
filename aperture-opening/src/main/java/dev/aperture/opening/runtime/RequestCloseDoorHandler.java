package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.capability.OpenableCapability;
import dev.aperture.runtime.model.capability.StandardCapabilities;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.RequestCloseCommand;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;

import java.util.List;
import java.util.Map;

public final class RequestCloseDoorHandler implements CommandHandler<RequestCloseCommand> {
	@Override public String commandType() { return "aperture:request_close"; }
	@Override public Class<RequestCloseCommand> commandClass() { return RequestCloseCommand.class; }

	@Override
	public CommandResult handle(CommandEnvelope<RequestCloseCommand> envelope, CommandContext context) {
		OpenableCapability openable = context.capabilities().requireCapability(StandardCapabilities.OPENABLE);
		if (!openable.canRequestClose() && openable.targetRatio() == 0) {
			return CommandResult.rejected("door.close_denied", "Door cannot be closed");
		}
		return CommandResult.accepted(List.of(new StatePatch(context.state().revision(), Map.of(
			DoorStateSchema.TARGET_OPEN_RATIO, StateValue.number(0),
			DoorStateSchema.MOTION, StateValue.enumeration("closing"),
			DoorStateSchema.LAST_INTERACTOR, StateValue.string(envelope.actor().id())
		), envelope.timestamp())));
	}
}
