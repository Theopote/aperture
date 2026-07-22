package dev.aperture.editor.model.command;

import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.RequestCloseCommand;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.command.SetLockCommand;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;

/** Shared mapping for standard runtime capability actions. */
public final class StandardRuntimeActionResolver implements RuntimeActionResolver {
	@Override public ArchitecturalCommand resolve(ArchitecturalObjectId objectId, String actionId) {
		ObjectRef target = new ObjectRef(objectId);
		return switch (actionId) {
			case "request_open" -> new RequestOpenCommand(target);
			case "request_close" -> new RequestCloseCommand(target);
			case "set_locked" -> new SetLockCommand(target, true);
			case "set_unlocked" -> new SetLockCommand(target, false);
			default -> throw new IllegalArgumentException("Unsupported runtime action: " + actionId);
		};
	}
}
