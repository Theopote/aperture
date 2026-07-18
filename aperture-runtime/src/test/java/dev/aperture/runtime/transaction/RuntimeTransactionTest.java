package dev.aperture.runtime.transaction;

import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.lifecycle.DefaultRuntimeObjectSession;
import dev.aperture.runtime.lifecycle.InMemoryRuntimeObjectRepository;
import dev.aperture.runtime.lifecycle.KinematicModel;
import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.event.ArchitecturalEvent;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class RuntimeTransactionTest {
	private static final Instant NOW = Instant.parse("2026-07-18T13:00:00Z");

	@Test
	void atomicallyCommitsStateObjectRevisionAndEventSequence() {
		Fixture fixture = fixture();
		ArchitecturalEvent event = new ArchitecturalEvent() { };
		RuntimeMutation mutation = new RuntimeMutation(List.of(new StatePatch(StateRevision.INITIAL, Map.of(
			DoorStateSchema.TARGET_OPEN_RATIO, StateValue.number(1),
			DoorStateSchema.MOTION, StateValue.enumeration("opening")
		), NOW)), List.of(event), List.of(), List.of());

		RuntimeCommitResult result = fixture.transaction.commit(fixture.initial, 0, mutation);

		assertEquals(RuntimeCommitResult.Status.COMMITTED, result.status());
		RuntimeObjectSession committed = fixture.repository.require(fixture.initial.instance().objectId());
		assertNotSame(fixture.initial, committed);
		assertEquals(1, committed.objectRevision());
		assertEquals(new StateRevision(1), committed.stateRevision());
		assertEquals(1, committed.eventSequence());
		assertEquals(StateValue.number(1), committed.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
		assertEquals(StateValue.number(0), fixture.initial.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
		assertEquals(event, result.change().orElseThrow().events().getFirst());
	}

	@Test
	void rejectsWrongObjectRevisionWithoutReplacingSession() {
		Fixture fixture = fixture();
		RuntimeMutation mutation = mutation(DoorStateSchema.LOCKED, StateValue.bool(true));

		RuntimeCommitResult result = fixture.transaction.commit(fixture.initial, 4, mutation);

		assertEquals(RuntimeCommitResult.Status.REJECTED, result.status());
		assertEquals("runtime.object_revision_conflict", result.code());
		assertSame(fixture.initial, fixture.repository.require(fixture.initial.instance().objectId()));
	}

	@Test
	void rejectsInvalidSchemaUpdateWithoutApplyingValidSiblingUpdate() {
		Fixture fixture = fixture();
		RuntimeMutation mutation = new RuntimeMutation(List.of(new StatePatch(StateRevision.INITIAL, Map.of(
			DoorStateSchema.LOCKED, StateValue.bool(true),
			DoorStateSchema.OPEN_RATIO, StateValue.number(2)
		), NOW)), List.of(), List.of(), List.of());

		RuntimeCommitResult result = fixture.transaction.commit(fixture.initial, 0, mutation);

		assertEquals(RuntimeCommitResult.Status.REJECTED, result.status());
		RuntimeObjectSession unchanged = fixture.repository.require(fixture.initial.instance().objectId());
		assertSame(fixture.initial, unchanged);
		assertEquals(StateValue.bool(false), unchanged.state().value(DoorStateSchema.LOCKED));
		assertEquals(StateRevision.INITIAL, unchanged.stateRevision());
		assertEquals(0, unchanged.eventSequence());
	}

	@Test
	void rejectsStateRevisionConflictWithoutPartialCommit() {
		Fixture fixture = fixture();
		StatePatch stale = new StatePatch(new StateRevision(3),
			Map.of(DoorStateSchema.LOCKED, StateValue.bool(true)), NOW);

		RuntimeCommitResult result = fixture.transaction.commit(fixture.initial, 0,
			new RuntimeMutation(List.of(stale), List.of(), List.of(), List.of()));

		assertEquals(RuntimeCommitResult.Status.REJECTED, result.status());
		assertSame(fixture.initial, fixture.repository.require(fixture.initial.instance().objectId()));
	}

	private static RuntimeMutation mutation(String property, StateValue value) {
		return new RuntimeMutation(List.of(new StatePatch(StateRevision.INITIAL,
			Map.of(property, value), NOW)), List.of(), List.of(), List.of());
	}

	private static Fixture fixture() {
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA, DoorCapabilities::from, List.of(), KinematicModel.EMPTY);
		RuntimeObjectSession session = DefaultRuntimeObjectSession.create(instance, configuration);
		InMemoryRuntimeObjectRepository repository = new InMemoryRuntimeObjectRepository();
		repository.add(session);
		return new Fixture(repository, session, new RuntimeTransaction(repository));
	}

	private record Fixture(
		InMemoryRuntimeObjectRepository repository,
		RuntimeObjectSession initial,
		RuntimeTransaction transaction
	) { }
}
