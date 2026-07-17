package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.capability.LockableCapability;
import dev.aperture.runtime.model.capability.StandardCapabilities;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.SetLockCommand;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;

import java.util.List;
import java.util.Map;

public final class SetDoorLockHandler implements CommandHandler<SetLockCommand> {
	@Override public String commandType() { return "aperture:set_lock"; }
	@Override public Class<SetLockCommand> commandClass() { return SetLockCommand.class; }

	@Override
	public CommandResult handle(CommandEnvelope<SetLockCommand> envelope, CommandContext context) {
		LockableCapability lockable = context.capabilities().requireCapability(StandardCapabilities.LOCKABLE);
		if (!lockable.canChangeLock()) return CommandResult.rejected("door.lock_denied", "Door must be closed and enabled");
		return CommandResult.accepted(List.of(new StatePatch(context.state().revision(),
			Map.of(DoorStateSchema.LOCKED, StateValue.bool(envelope.command().locked())), envelope.timestamp())));
	}
}
