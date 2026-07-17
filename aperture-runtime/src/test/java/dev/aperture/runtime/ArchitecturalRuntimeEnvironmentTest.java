package dev.aperture.runtime;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.object.ArchitecturalObject;
import dev.aperture.runtime.diagnostic.RuntimeDiagnostics;
import dev.aperture.runtime.event.RuntimeEvent;
import dev.aperture.runtime.event.RuntimeEventBus;
import dev.aperture.runtime.pipeline.OpeningInstanceRepository;
import dev.aperture.runtime.pipeline.OpeningRuntimeBehavior;
import dev.aperture.runtime.pipeline.RuntimeInteraction;
import dev.aperture.runtime.pipeline.RuntimePipeline;
import dev.aperture.runtime.replication.RuntimeReplicator;
import dev.aperture.runtime.schedule.RuntimeTickScheduler;
import dev.aperture.runtime.state.RuntimeObjectRegistry;
import dev.aperture.runtime.transaction.RuntimeTransactionManager;
import dev.aperture.runtime.world.RuntimeWorldQuery;
import dev.aperture.runtime.world.WorldQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitecturalRuntimeEnvironmentTest {
	private InMemoryOpeningInstanceStore stateStore;
	private RuntimeObjectRegistry objects;
	private RuntimeEventBus events;
	private RuntimeDiagnostics diagnostics;
	private List<RuntimeEvent> publishedEvents;
	private List<ArchitecturalObject> replicated;
	private ArchitecturalRuntimeEnvironment environment;

	@BeforeEach
	void setUp() {
		OpeningTypeRegistry definitions = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(definitions::register);
		stateStore = new InMemoryOpeningInstanceStore();
		objects = new RuntimeObjectRegistry(new OpeningInstanceRepository(stateStore));
		RuntimePipeline pipeline = new RuntimePipeline(
			List.of(new OpeningRuntimeBehavior(definitions)),
			objects
		);
		events = new RuntimeEventBus();
		diagnostics = new RuntimeDiagnostics();
		publishedEvents = new ArrayList<>();
		replicated = new ArrayList<>();
		events.subscribe(publishedEvents::add);
		environment = createEnvironment(
			pipeline,
			(previous, current) -> replicated.add(current),
			RuntimeWorldQuery.empty()
		);
	}

	@Test
	void ownsLiveObjectStateAndRunsTransactionalInteractionLifecycle() {
		OpeningInstance door = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID).build();
		environment.register(door);

		var result = environment.interact(door.instanceId(), RuntimeInteraction.of("aperture:toggle"));

		OpeningInstance current = assertInstanceOf(OpeningInstance.class, environment.find(door.instanceId()).orElseThrow());
		assertTrue(result.changed());
		assertEquals(1.0, current.state().openRatio());
		assertEquals(current, stateStore.findById(door.instanceId()).orElseThrow());
		assertEquals(List.of(current), replicated);
		assertInstanceOf(RuntimeEvent.InteractionCompleted.class, publishedEvents.get(0));
		assertEquals(1, diagnostics.snapshot().interactions());
		assertEquals(1, diagnostics.snapshot().transitions());
	}

	@Test
	void scheduledInteractionRunsOnItsDueRuntimeTick() {
		OpeningInstance door = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID).build();
		environment.register(door);
		environment.scheduleInteraction(2, door.instanceId(), RuntimeInteraction.of("aperture:open"));

		assertEquals(0, environment.tick());
		assertEquals(0.0, ((OpeningInstance) environment.find(door.instanceId()).orElseThrow()).state().openRatio());
		assertEquals(1, environment.tick());
		assertEquals(1.0, ((OpeningInstance) environment.find(door.instanceId()).orElseThrow()).state().openRatio());
		assertEquals(2, diagnostics.snapshot().ticks());
		assertEquals(1, diagnostics.snapshot().scheduledTasks());
	}

	@Test
	void rejectedInteractionProducesEventAndDiagnosticsWithoutChangingState() {
		OpeningInstance fixedWindow = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID).build();
		environment.register(fixedWindow);

		assertThrows(
			IllegalArgumentException.class,
			() -> environment.interact(fixedWindow.instanceId(), RuntimeInteraction.of("aperture:toggle"))
		);

		assertEquals(fixedWindow, environment.find(fixedWindow.instanceId()).orElseThrow());
		assertEquals(1, diagnostics.snapshot().rejectedInteractions());
		assertInstanceOf(RuntimeEvent.InteractionRejected.class, publishedEvents.get(0));
		assertTrue(replicated.isEmpty());
	}

	@Test
	void exposesTypedReadOnlyWorldQueryBoundary() {
		RuntimeWorldQuery world = new RuntimeWorldQuery() {
			@Override
			public <T> Optional<T> query(WorldQuery<T> query) {
				return query.id().equals("minecraft:sky_light")
					? Optional.of(query.resultType().cast(12))
					: Optional.empty();
			}
		};
		OpeningTypeRegistry definitions = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(definitions::register);
		RuntimePipeline pipeline = new RuntimePipeline(
			List.of(new OpeningRuntimeBehavior(definitions)),
			objects
		);
		ArchitecturalRuntimeEnvironment queried = createEnvironment(pipeline, RuntimeReplicator.noop(), world);

		Optional<Integer> light = queried.world().query(
			new WorldQuery<>("minecraft:sky_light", Integer.class, Map.of("x", 0, "y", 64, "z", 0))
		);

		assertEquals(Optional.of(12), light);
	}

	private ArchitecturalRuntimeEnvironment createEnvironment(
		RuntimePipeline pipeline,
		RuntimeReplicator replicator,
		RuntimeWorldQuery world
	) {
		return new ArchitecturalRuntimeEnvironment(
			objects,
			pipeline,
			new RuntimeTransactionManager(),
			events,
			new RuntimeTickScheduler(),
			world,
			replicator,
			diagnostics
		);
	}
}
