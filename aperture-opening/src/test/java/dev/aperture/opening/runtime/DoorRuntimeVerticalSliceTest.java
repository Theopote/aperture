package dev.aperture.opening.runtime;

import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.behavior.BehaviorContext;
import dev.aperture.runtime.model.behavior.BehaviorResult;
import dev.aperture.runtime.model.behavior.DeterministicBehaviorEngine;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.EventContext;
import dev.aperture.runtime.model.event.EventEnvelope;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.PlayerInteractEvent;
import dev.aperture.runtime.model.event.SpatialRef;
import dev.aperture.runtime.model.event.StandardEventTypes;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DoorRuntimeVerticalSliceTest {
	@Test
	void interactionBecomesCommandThenStateTransitionAndTick() {
		ArchitecturalObjectId id = ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, id, ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(
				DoorStateSchema.OPEN_RATIO, StateValue.number(0),
				DoorStateSchema.LOCKED, StateValue.bool(false),
				DoorStateSchema.ENABLED, StateValue.bool(true)
			), 0, Map.of());
		DoorRuntimeDefinition definition = new DoorRuntimeDefinition(instance.typeId(), Map.of());
		DoorRuntimeObject door = DoorRuntimeObject.activate(definition, instance);
		WorldRef world = new WorldRef("test:world");
		ActorRef actor = new ActorRef("test:player");
		ObjectRef target = new ObjectRef(id);
		EventEnvelope<PlayerInteractEvent> interaction = EventEnvelope.create(
			StandardEventTypes.PLAYER_INTERACT,
			new PlayerInteractEvent(target, "toggle_open", new SpatialRef(world, Vec3d.ZERO, "panel")),
			new EventContext(world, actor, target, java.util.UUID.randomUUID(), null, Map.of()),
			Instant.EPOCH, 0);

		BehaviorResult behavior = new DeterministicBehaviorEngine().evaluate(door.behaviors(), new BehaviorContext(
			instance, door.state(), instance.parameterOverrides(), door.capabilities(), interaction,
			WorldQueryExecutor.unavailable()));
		assertEquals(1, behavior.commands().size());
		assertInstanceOf(dev.aperture.runtime.model.command.RequestOpenCommand.class, behavior.commands().getFirst());

		ArchitecturalCommand command = behavior.commands().getFirst();
		CommandEnvelope<ArchitecturalCommand> commandEnvelope = CommandEnvelope.create(
			command, actor, world, instance.revision(), Instant.EPOCH);
		DefaultCommandBus bus = new DefaultCommandBus(List.of(
			new RequestOpenDoorHandler(), new RequestCloseDoorHandler(), new SetDoorLockHandler()));
		CommandResult commandResult = bus.dispatch(commandEnvelope, new CommandContext(
			instance, door.state(), door.capabilities(), WorldQueryExecutor.unavailable()));
		assertEquals(CommandResult.Status.ACCEPTED, commandResult.status());

		RuntimeState requested = door.state().apply(commandResult.statePatches().getFirst());
		assertEquals(new StateValue.NumberValue(1), requested.value(DoorStateSchema.TARGET_OPEN_RATIO));
		RuntimeState halfway = requested.apply(DoorRuntimeTick.advance(
			requested, Duration.ofMillis(500), 1.0, Instant.ofEpochMilli(500)).orElseThrow());
		assertEquals(new StateValue.NumberValue(0.5), halfway.value(DoorStateSchema.OPEN_RATIO));
		RuntimeState opened = halfway.apply(DoorRuntimeTick.advance(
			halfway, Duration.ofMillis(500), 1.0, Instant.ofEpochSecond(1)).orElseThrow());
		assertEquals(new StateValue.NumberValue(1), opened.value(DoorStateSchema.OPEN_RATIO));
		assertEquals(new StateValue.EnumValue("idle"), opened.value(DoorStateSchema.MOTION));
	}
}
