package dev.aperture.runtime.lifecycle;

import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorKinematics;
import dev.aperture.opening.runtime.DoorRuntimeTick;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.opening.runtime.RequestOpenDoorHandler;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.kinematic.DynamicSpatialIndex;
import dev.aperture.runtime.kinematic.KinematicPose;
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
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorDynamicRuntimeTest {
	private static final ComponentPath PANEL = new ComponentPath("door.panel");
	private static final ComponentPath HANDLE = new ComponentPath("door.panel.handle");

	@Test
	void commandAndTicksDriveOnePoseForRenderCollisionPickingAndRestore() {
		ArchitecturalObjectInstance instance = doorInstance();
		ArchitecturalRuntime runtime = runtime();
		runtime.create(instance);
		runtime.submit(CommandEnvelope.create(
			new RequestOpenCommand(new ObjectRef(instance.objectId())), new ActorRef("test:player"),
			new WorldRef("test:world"), 0, Instant.EPOCH));

		runtime.tick(new RuntimeTickContext(Duration.ofSeconds(1), Instant.ofEpochSecond(1)));
		RuntimeObjectSession moving = runtime.find(instance.objectId()).orElseThrow();
		assertEquals(StateValue.number(0.5), moving.state().value(DoorStateSchema.OPEN_RATIO));
		assertEquals(StateValue.enumeration("opening"), moving.state().value(DoorStateSchema.MOTION));
		assertEquals(2, moving.objectRevision());
		assertEquals(2, moving.stateRevision().value());

		KinematicPose pose = KinematicPose.evaluate(moving);
		assertEquals(Math.toRadians(45), pose.transformFor(HANDLE).rotationRadians(), 1.0e-9);
		BoundingBox localPanel = new BoundingBox(Vec3d.ZERO, new Vec3d(1000, 2000, 100));
		DynamicSpatialIndex spatial = DynamicSpatialIndex.evaluate(Map.of(PANEL, localPanel), pose);
		assertNotEquals(localPanel, spatial.collisionBounds().getFirst().worldBounds());
		assertEquals(PANEL, spatial.pick(new Vec3d(300, 1000, -1000), new Vec3d(0, 0, 1), 2000)
			.orElseThrow().component());

		runtime.tick(new RuntimeTickContext(Duration.ofSeconds(1), Instant.ofEpochSecond(2)));
		RuntimeObjectSession opened = runtime.find(instance.objectId()).orElseThrow();
		assertEquals(StateValue.number(1), opened.state().value(DoorStateSchema.OPEN_RATIO));
		assertEquals(StateValue.enumeration("idle"), opened.state().value(DoorStateSchema.MOTION));
		long completedRevision = opened.objectRevision();
		runtime.tick(new RuntimeTickContext(Duration.ofSeconds(1), Instant.ofEpochSecond(3)));
		assertEquals(completedRevision, runtime.find(instance.objectId()).orElseThrow().objectRevision());

		var snapshot = opened.snapshot();
		runtime.unload(instance.objectId());
		RuntimeObjectSession restored = runtime.restore(snapshot);
		assertEquals(StateValue.number(1), restored.state().value(DoorStateSchema.OPEN_RATIO));
		assertEquals(StateValue.number(1), restored.state().value(DoorStateSchema.TARGET_OPEN_RATIO));
		assertEquals(Math.toRadians(90), KinematicPose.evaluate(restored)
			.transformFor(PANEL).rotationRadians(), 1.0e-9);
		assertTrue(restored.dirtyFlags().equals(DirtyFlags.CLEAN));
	}

	private static ArchitecturalRuntime runtime() {
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA,
			DoorCapabilities::from,
			List.of(),
			new KinematicModel(List.of(DoorKinematics.swingPanel(PANEL.value(), Vec3d.ZERO, false))),
			DoorRuntimeTick.atSpeed(0.5));
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
