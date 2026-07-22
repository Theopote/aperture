package dev.aperture.editor.model.command;

import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.command.SetLockCommand;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeActionResolverTest {
	private final StandardRuntimeActionResolver resolver = new StandardRuntimeActionResolver();
	private final ArchitecturalObjectId objectId = ArchitecturalObjectId.random();

	@Test void resolvesFrontendNeutralActionIds() {
		assertInstanceOf(RequestOpenCommand.class, resolver.resolve(objectId, "request_open"));
		assertTrue(((SetLockCommand)resolver.resolve(objectId, "set_locked")).locked());
		assertFalse(((SetLockCommand)resolver.resolve(objectId, "set_unlocked")).locked());
	}

	@Test void rejectsUnknownActionInsteadOfSilentlyDrifting() {
		assertThrows(IllegalArgumentException.class, () -> resolver.resolve(objectId, "unknown"));
	}
}
