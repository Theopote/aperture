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
import dev.aperture.runtime.model.world.WorldEffectExecutor;
import dev.aperture.runtime.model.world.WorldEffectResult;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import dev.aperture.runtime.transaction.RuntimeCommitResult;
import dev.aperture.runtime.transaction.RuntimeMutation;
import dev.aperture.runtime.transaction.RuntimeTransaction;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/** Default lifecycle coordinator. Transactional mutation is completed in K2.2 Phase 2. */
public final class DefaultArchitecturalRuntime implements ArchitecturalRuntime {
	private final RuntimeObjectRepository repository;
	private final RuntimeObjectConfigurationResolver configurations;
	private final CommandBus commandBus;
	private final WorldQueryExecutor worldQuery;
	private final WorldEffectExecutor worldEffects;
	private final RuntimeTransaction runtimeTransaction;

	public DefaultArchitecturalRuntime(
		RuntimeObjectRepository repository,
		RuntimeObjectConfigurationResolver configurations,
		CommandBus commandBus,
		WorldQueryExecutor worldQuery
	) {
		this(repository, configurations, commandBus, worldQuery, effect -> WorldEffectResult.applied());
	}

	public DefaultArchitecturalRuntime(
		RuntimeObjectRepository repository,
		RuntimeObjectConfigurationResolver configurations,
		CommandBus commandBus,
		WorldQueryExecutor worldQuery,
		WorldEffectExecutor worldEffects
	) {
		this.repository = Objects.requireNonNull(repository, "repository");
		this.configurations = Objects.requireNonNull(configurations, "configurations");
		this.commandBus = Objects.requireNonNull(commandBus, "commandBus");
		this.worldQuery = Objects.requireNonNull(worldQuery, "worldQuery");
		this.worldEffects = Objects.requireNonNull(worldEffects, "worldEffects");
		this.runtimeTransaction = new RuntimeTransaction(repository);
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
		CommandResult evaluated = commandBus.dispatch(envelope, context(session));
		if (evaluated.status() == CommandResult.Status.REJECTED) return evaluated;
		RuntimeCommitResult committed = runtimeTransaction.commit(
			session, envelope.expectedObjectRevision(), RuntimeMutation.from(evaluated));
		if (committed.status() == RuntimeCommitResult.Status.REJECTED) {
			return CommandResult.rejected(committed.code(), committed.message());
		}
		committed.change().orElseThrow().worldEffects().forEach(worldEffects::execute);
		return evaluated;
	}

	@Override
	public TransactionResult submit(CommandTransaction transaction) {
		Objects.requireNonNull(transaction, "transaction");
		ArchitecturalObjectId target = transaction.commands().getFirst().command().target().objectId();
		if (transaction.commands().stream().anyMatch(command ->
			!target.equals(command.command().target().objectId()))) {
			return rejectedTransaction(transaction, "transaction.multiple_objects",
				"Runtime transaction must target one active object");
		}
		RuntimeObjectSession session = repository.require(target);
		TransactionResult evaluated = commandBus.dispatch(transaction, context(session));
		if (evaluated.status() == TransactionResult.Status.REJECTED) return evaluated;
		RuntimeMutation mutation = new RuntimeMutation(
			evaluated.commandResults().stream().flatMap(result -> result.statePatches().stream()).toList(),
			evaluated.commandResults().stream().flatMap(result -> result.events().stream()).toList(),
			evaluated.commandResults().stream().flatMap(result -> result.worldEffects().stream()).toList(),
			java.util.List.of());
		RuntimeCommitResult committed = runtimeTransaction.commit(
			session, transaction.commands().getFirst().expectedObjectRevision(), mutation);
		if (committed.status() == RuntimeCommitResult.Status.REJECTED) {
			return rejectedTransaction(transaction, committed.code(), committed.message());
		}
		committed.change().orElseThrow().worldEffects().forEach(worldEffects::execute);
		return evaluated;
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

	private static TransactionResult rejectedTransaction(
		CommandTransaction transaction, String code, String message
	) {
		return new TransactionResult(TransactionResult.Status.REJECTED, transaction.commands().stream()
			.map(ignored -> CommandResult.rejected(code, message)).toList());
	}

	private RuntimeObjectConfiguration requireConfiguration(ArchitecturalObjectInstance instance) {
		return Objects.requireNonNull(configurations.resolve(instance),
			() -> "No runtime configuration for " + instance.typeId());
	}

	private CommandContext context(RuntimeObjectSession session) {
		return new CommandContext(session.instance(), session.state(), session.capabilities(), worldQuery);
	}
}
