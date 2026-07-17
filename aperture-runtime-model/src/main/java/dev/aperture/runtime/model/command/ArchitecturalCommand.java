package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;

/** Intent to mutate architectural runtime state; execution belongs to the future Command Bus. */
public interface ArchitecturalCommand {
	String commandType();
	ObjectRef target();
}
