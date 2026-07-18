package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.CommandTransaction;
import dev.aperture.runtime.model.command.TransactionResult;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;

import java.util.Collection;
import java.util.Optional;

/** Server-authoritative lifecycle boundary for all architectural families. */
public interface ArchitecturalRuntime {
	RuntimeObjectSession create(ArchitecturalObjectInstance instance);
	RuntimeObjectSession restore(ArchitecturalObjectSnapshot snapshot);
	CommandResult submit(CommandEnvelope<?> envelope);
	TransactionResult submit(CommandTransaction transaction);
	void tick(RuntimeTickContext context);
	void unload(ArchitecturalObjectId objectId);
	void remove(ArchitecturalObjectId objectId);
	Optional<RuntimeObjectSession> find(ArchitecturalObjectId objectId);
	Collection<RuntimeObjectSession> activeObjects();
}
