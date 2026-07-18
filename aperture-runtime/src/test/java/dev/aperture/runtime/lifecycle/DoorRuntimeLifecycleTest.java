package dev.aperture.runtime.lifecycle;

import dev.aperture.geometry.kinematic.KinematicEvaluator;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorKinematics;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.opening.runtime.ManualDoorInteractionBehavior;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorRuntimeLifecycleTest {
	@Test
	void createsOwnsSnapshotsUnloadsAndRestoresDoorSession() {
		ArchitecturalObjectInstance door = doorInstance();
		ArchitecturalRuntime runtime = runtime();

		RuntimeObjectSession created = runtime.create(door);

		assertSame(created, runtime.find(door.objectId()).orElseThrow());
		assertEquals(1, runtime.activeObjects().size());
		assertEquals(StateValue.number(0), created.state().value(DoorStateSchema.OPEN_RATIO));
		assertTrue(created.dirtyFlags().persistence());
		assertEquals(1, created.kinematics().parts().size());
		assertEquals(0, KinematicEvaluator.evaluate(created.kinematics().parts().getFirst(),
			Map.of(DoorStateSchema.OPEN_RATIO, 0.0)).rotationRadians());

		var snapshot = created.snapshot();
		runtime.unload(door.objectId());
		assertTrue(runtime.find(door.objectId()).isEmpty());

		RuntimeObjectSession restored = runtime.restore(snapshot);
		assertEquals(snapshot.instance(), restored.instance());
		assertEquals(created.state(), restored.state());
		assertFalse(restored.dirtyFlags().persistence());
		assertEquals(created.snapshot(), restored.snapshot());
	}

	@Test
	void refusesASecondAuthoritativeSessionForTheSameObject() {
		ArchitecturalObjectInstance door = doorInstance();
		ArchitecturalRuntime runtime = runtime();
		runtime.create(door);

		assertThrows(IllegalStateException.class, () -> runtime.create(door));
	}

	@Test
	void removeAndUnloadBothDeactivateButRemainDistinctRuntimeOperations() {
		ArchitecturalObjectInstance first = doorInstance();
		ArchitecturalObjectInstance second = doorInstance();
		ArchitecturalRuntime runtime = runtime();
		runtime.create(first);
		runtime.create(second);

		runtime.unload(first.objectId());
		runtime.remove(second.objectId());

		assertTrue(runtime.activeObjects().isEmpty());
	}

	private static ArchitecturalRuntime runtime() {
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA,
			DoorCapabilities::from,
			List.of(new ManualDoorInteractionBehavior()),
			new KinematicModel(List.of(DoorKinematics.swingPanel(
				"door.panel", Vec3d.ZERO, false)))
		);
		return new DefaultArchitecturalRuntime(
			new InMemoryRuntimeObjectRepository(),
			instance -> instance.typeId().equals(ArchitecturalTypeId.parse("aperture:door"))
				? configuration : null,
			new DefaultCommandBus(List.of()),
			WorldQueryExecutor.unavailable()
		);
	}

	private static ArchitecturalObjectInstance doorInstance() {
		return new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of()
		);
	}
}
