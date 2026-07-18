package dev.aperture.runtime.lifecycle;

import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.opening.runtime.RequestOpenDoorHandler;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultArchitecturalRuntimeTransactionTest {
	private static final Instant NOW = Instant.parse("2026-07-18T13:10:00Z");

	@Test
	void acceptedCommandCommitsOnlyThroughRuntimeTransaction() {
		ArchitecturalObjectInstance instance = doorInstance();
		ArchitecturalRuntime runtime = runtime();
		RuntimeObjectSession before = runtime.create(instance);

		CommandResult result = runtime.submit(envelope(instance.objectId(), 0));

		assertEquals(CommandResult.Status.ACCEPTED, result.status());
		RuntimeObjectSession after = runtime.find(instance.objectId()).orElseThrow();
		assertEquals(1, after.objectRevision());
		assertEquals(new StateRevision(1), after.stateRevision());
		assertEquals(StateValue.number(1), after.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
		assertEquals(StateValue.enumeration("opening"), after.state().value(DoorStateSchema.MOTION));
		assertEquals(StateValue.number(0), before.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
	}

	@Test
	void staleCommandIsRejectedAndLeavesAuthoritativeSessionUntouched() {
		ArchitecturalObjectInstance instance = doorInstance();
		ArchitecturalRuntime runtime = runtime();
		runtime.create(instance);
		runtime.submit(envelope(instance.objectId(), 0));
		RuntimeObjectSession committed = runtime.find(instance.objectId()).orElseThrow();

		CommandResult stale = runtime.submit(envelope(instance.objectId(), 0));

		assertEquals(CommandResult.Status.REJECTED, stale.status());
		assertSame(committed, runtime.find(instance.objectId()).orElseThrow());
		assertEquals(1, committed.objectRevision());
	}

	private static CommandEnvelope<RequestOpenCommand> envelope(ArchitecturalObjectId id, long revision) {
		return CommandEnvelope.create(new RequestOpenCommand(new ObjectRef(id)),
			new ActorRef("test:player"), new WorldRef("test:world"), revision, NOW);
	}

	private static ArchitecturalRuntime runtime() {
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA, DoorCapabilities::from, List.of(), KinematicModel.EMPTY);
		return new DefaultArchitecturalRuntime(
			new InMemoryRuntimeObjectRepository(), ignored -> configuration,
			new DefaultCommandBus(List.of(new RequestOpenDoorHandler())), WorldQueryExecutor.unavailable());
	}

	private static ArchitecturalObjectInstance doorInstance() {
		return new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
	}
}
