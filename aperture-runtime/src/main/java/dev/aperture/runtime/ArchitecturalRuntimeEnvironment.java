package dev.aperture.runtime;

import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.diagnostic.RuntimeDiagnostics;
import dev.aperture.runtime.event.RuntimeEvent;
import dev.aperture.runtime.event.RuntimeEventBus;
import dev.aperture.runtime.pipeline.RuntimeCapability;
import dev.aperture.runtime.pipeline.RuntimeInteraction;
import dev.aperture.runtime.pipeline.RuntimeEvaluationContext;
import dev.aperture.runtime.pipeline.RuntimePipeline;
import dev.aperture.runtime.pipeline.RuntimeResult;
import dev.aperture.runtime.replication.RuntimeReplicator;
import dev.aperture.runtime.schedule.RuntimeTickScheduler;
import dev.aperture.runtime.state.RuntimeObjectRegistry;
import dev.aperture.runtime.transaction.RuntimeTransactionManager;
import dev.aperture.runtime.world.RuntimeWorldQuery;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Live architectural-object environment. It owns runtime identity, transactional
 * interaction, events, scheduling, replication, world queries, and diagnostics.
 */
public final class ArchitecturalRuntimeEnvironment {
	private final RuntimeObjectRegistry objects;
	private final RuntimePipeline pipeline;
	private final RuntimeTransactionManager transactions;
	private final RuntimeEventBus events;
	private final RuntimeTickScheduler scheduler;
	private final RuntimeWorldQuery world;
	private final RuntimeReplicator replicator;
	private final RuntimeDiagnostics diagnostics;

	public ArchitecturalRuntimeEnvironment(
		RuntimeObjectRegistry objects,
		RuntimePipeline pipeline,
		RuntimeTransactionManager transactions,
		RuntimeEventBus events,
		RuntimeTickScheduler scheduler,
		RuntimeWorldQuery world,
		RuntimeReplicator replicator,
		RuntimeDiagnostics diagnostics
	) {
		this.objects = Objects.requireNonNull(objects, "objects");
		this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
		this.transactions = Objects.requireNonNull(transactions, "transactions");
		this.events = Objects.requireNonNull(events, "events");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.world = Objects.requireNonNull(world, "world");
		this.replicator = Objects.requireNonNull(replicator, "replicator");
		this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
	}

	public ArchitecturalObject register(ArchitecturalObject object) {
		return objects.register(object);

	}
	public boolean unregister(UUID objectId) {
		return transactions.execute(objectId, () -> objects.unregister(objectId));
	}

	public Optional<ArchitecturalObject> find(UUID objectId) {
		return objects.find(objectId);
	}

	public Set<RuntimeCapability> capabilities(UUID objectId) {
		return pipeline.capabilities(objects.require(objectId), evaluationContext());
	}

	public RuntimeResult interact(UUID objectId, RuntimeInteraction interaction) {
		Objects.requireNonNull(objectId, "objectId");
		Objects.requireNonNull(interaction, "interaction");
		RuntimeResult result;
		try {
			result = transactions.execute(objectId, () -> {
				ArchitecturalObject current = objects.require(objectId);
				return pipeline.process(current, interaction, evaluationContext());
			});
		} catch (RuntimeException failure) {
			diagnostics.recordRejected(failure);
			events.publish(new RuntimeEvent.InteractionRejected(
				objectId,
				interaction.action(),
				failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()
			));
			throw failure;
		}
		diagnostics.recordInteraction(result.changed());
		if (result.changed()) {
			replicator.replicate(result.previous(), result.current());
		}
		events.publish(new RuntimeEvent.InteractionCompleted(result));
		return result;

	}

	public void scheduleInteraction(long delayTicks, UUID objectId, RuntimeInteraction interaction) {
		scheduler.schedule(delayTicks, () -> interact(objectId, interaction));
	}

	public int tick() {
		int executed = scheduler.tick();
		diagnostics.recordTick(executed);
		events.publish(new RuntimeEvent.TickAdvanced(scheduler.currentTick(), executed));
		return executed;
	}

	public RuntimeEventBus events() {
		return events;
	}

	public RuntimeWorldQuery world() {
		return world;
	}


	private RuntimeEvaluationContext evaluationContext() {
		return new RuntimeEvaluationContext(scheduler.currentTick(), world);
	}
	public RuntimeDiagnostics.Snapshot diagnostics() {
		return diagnostics.snapshot();
	}
}
