package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.command.CommandBus;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.CommandTransaction;
import dev.aperture.runtime.model.command.TransactionResult;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.world.WorldQueryExecutor;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/** Default lifecycle coordinator. Transactional mutation is completed in K2.2 Phase 2. */
public final class DefaultArchitecturalRuntime implements ArchitecturalRuntime {
	private final RuntimeObjectRepository repository;
	private final RuntimeObjectConfigurationResolver configurations;
	private final CommandBus commandBus;
	private final WorldQueryExecutor worldQuery;

	public DefaultArchitecturalRuntime(
		RuntimeObjectRepository repository,
		RuntimeObjectConfigurationResolver configurations,
		CommandBus commandBus,
		WorldQueryExecutor worldQuery
	) {
		this.repository = Objects.requireNonNull(repository, "repository");
		this.configurations = Objects.requireNonNull(configurations, "configurations");
		this.commandBus = Objects.requireNonNull(commandBus, "commandBus");
		this.worldQuery = Objects.requireNonNull(worldQuery, "worldQuery");
	}

	@Override
	public RuntimeObjectSession create(ArchitecturalObjectInstance instance) {
		Objects.requireNonNull(instance, "instance");
		return repository.add(DefaultRuntimeObjectSession.create(instance, requireConfiguration(instance)));
	}

	@Override
	public RuntimeObjectSession restore(ArchitecturalObjectSnapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot");
		return repository.add(DefaultRuntimeObjectSession.restore(
			snapshot, requireConfiguration(snapshot.instance())));
	}

	@Override
	public CommandResult submit(CommandEnvelope<?> envelope) {
		Objects.requireNonNull(envelope, "envelope");
		RuntimeObjectSession session = repository.require(envelope.command().target().objectId());
		return commandBus.dispatch(envelope, context(session));
	}

	@Override
	public TransactionResult submit(CommandTransaction transaction) {
		Objects.requireNonNull(transaction, "transaction");
		ArchitecturalObjectId target = transaction.commands().getFirst().command().target().objectId();
		if (transaction.commands().stream().anyMatch(command ->
			!target.equals(command.command().target().objectId()))) {
			return new TransactionResult(TransactionResult.Status.REJECTED, transaction.commands().stream()
				.map(ignored -> CommandResult.rejected("transaction.multiple_objects",
					"Phase 1 transactions must target one active object"))
				.toList());
		}
		return commandBus.dispatch(transaction, context(repository.require(target)));
	}

	@Override public void tick(RuntimeTickContext context) { Objects.requireNonNull(context, "context"); }
	@Override public void unload(ArchitecturalObjectId objectId) { repository.unload(objectId); }

	@Override
	public void remove(ArchitecturalObjectId objectId) {
		// Phase 1 distinguishes intent; tombstone replication and durable deletion follow later.
		repository.unload(objectId);
	}

	@Override public Optional<RuntimeObjectSession> find(ArchitecturalObjectId objectId) { return repository.find(objectId); }
	@Override public Collection<RuntimeObjectSession> activeObjects() { return repository.activeObjects(); }

	private RuntimeObjectConfiguration requireConfiguration(ArchitecturalObjectInstance instance) {
		return Objects.requireNonNull(configurations.resolve(instance),
			() -> "No runtime configuration for " + instance.typeId());
	}

	private CommandContext context(RuntimeObjectSession session) {
		return new CommandContext(session.instance(), session.state(), session.capabilities(), worldQuery);
	}
}
