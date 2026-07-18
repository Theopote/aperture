package dev.aperture.fabric.runtime;

import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorKinematics;
import dev.aperture.opening.runtime.DoorRuntimeTick;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.opening.runtime.RequestOpenDoorHandler;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.kinematic.KinematicPose;
import dev.aperture.runtime.lifecycle.ArchitecturalRuntime;
import dev.aperture.runtime.lifecycle.DefaultArchitecturalRuntime;
import dev.aperture.runtime.lifecycle.InMemoryRuntimeObjectRepository;
import dev.aperture.runtime.lifecycle.KinematicModel;
import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.lifecycle.RuntimeTickContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.object.HostBinding;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricRuntimeLifecycleTest {
	@AfterEach
	void resetBridge() { FabricRuntimeLifecycle.resetForTesting(); }

	@Test
	void placeOpenSaveUnloadAndReloadRestoresAuthoritativeDoor() {
		ArchitecturalRuntime runtime = runtime();
		FabricRuntimeLifecycle.install(runtime);
		ArchitecturalObjectInstance placed = doorInstance();
		runtime.create(placed);
		runtime.submit(CommandEnvelope.create(
			new RequestOpenCommand(new ObjectRef(placed.objectId())), new ActorRef("test:player"),
			new WorldRef("test:world"), 0, Instant.EPOCH));
		runtime.tick(new RuntimeTickContext(Duration.ofMillis(600), Instant.ofEpochMilli(600)));

		var saved = FabricRuntimeLifecycle.snapshot(placed.objectId()).orElseThrow();
		FabricRuntimeLifecycle.unload(placed.objectId());
		assertTrue(FabricRuntimeLifecycle.find(placed.objectId()).isEmpty());

		var restored = FabricRuntimeLifecycle.activate(saved);
		assertEquals(placed.objectId(), restored.instance().objectId());
		assertEquals(placed.typeId(), restored.instance().typeId());
		assertEquals(placed.parameterOverrides(), restored.instance().parameterOverrides());
		assertEquals(placed.hostBindings(), restored.instance().hostBindings());
		assertEquals(StateValue.number(0.6), restored.state().value(DoorStateSchema.OPEN_RATIO));
		assertEquals(StateValue.number(1), restored.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
		assertEquals(2, restored.objectRevision());
		assertEquals(2, restored.stateRevision().value());
		assertEquals(Math.toRadians(54), KinematicPose.evaluate(restored)
			.transformFor(new dev.aperture.geometry.kinematic.ComponentPath("door.panel"))
			.rotationRadians(), 1.0e-9);
		assertEquals("geometry:door-900x2100", restored.instance().metadata().get("geometryFingerprint"));
	}

	@Test
	void removeDoesNotLeaveARecoverableActiveSession() {
		ArchitecturalRuntime runtime = runtime();
		FabricRuntimeLifecycle.install(runtime);
		ArchitecturalObjectInstance placed = doorInstance();
		runtime.create(placed);

		FabricRuntimeLifecycle.remove(placed.objectId());

		assertTrue(FabricRuntimeLifecycle.find(placed.objectId()).isEmpty());
	}

	private static ArchitecturalRuntime runtime() {
		RuntimeObjectConfiguration door = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA, DoorCapabilities::from, List.of(),
			new KinematicModel(List.of(DoorKinematics.swingPanel("door.panel", Vec3d.ZERO, false))),
			DoorRuntimeTick.atSpeed(1));
		return new DefaultArchitecturalRuntime(
			new InMemoryRuntimeObjectRepository(), ignored -> door,
			new DefaultCommandBus(List.of(new RequestOpenDoorHandler())), WorldQueryExecutor.unavailable());
	}

	private static ArchitecturalObjectInstance doorInstance() {
		ParameterSet parameters = ParameterSet.builder()
			.put("width", ParameterValue.length(900))
			.put("height", ParameterValue.length(2100))
			.build();
		HostBinding host = new HostBinding(ArchitecturalObjectId.random(), "wall.face",
			Transform3d.identity(), "embedded", ParameterSet.empty(), 3);
		return new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), parameters, Transform3d.identity(),
			List.of(host), Map.of(), 0, Map.of("geometryFingerprint", "geometry:door-900x2100"));
	}
}
