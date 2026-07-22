package dev.aperture.editor.model.command;

import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;

/** Resolves a frontend-neutral action ID into a durable architectural command. */
@FunctionalInterface
public interface RuntimeActionResolver {
	ArchitecturalCommand resolve(ArchitecturalObjectId objectId, String actionId);
}
