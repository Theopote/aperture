package dev.aperture.runtime.model.event;

import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDispatcherTest {
	@Test
	void dispatchesByTypedEventKeyInSubscriptionOrder() {
		SynchronousEventDispatcher dispatcher = new SynchronousEventDispatcher();
		List<String> calls = new ArrayList<>();
		EventSubscription first = dispatcher.subscribe(StandardEventTypes.PLAYER_INTERACT,
			event -> calls.add("first:" + event.event().interactionId()));
		dispatcher.subscribe(StandardEventTypes.PLAYER_INTERACT,
			event -> calls.add("second:" + event.sequence()));

		dispatcher.dispatch(interactionEnvelope(7));
		assertEquals(List.of("first:toggle_open", "second:7"), calls);
		assertTrue(first.active());

		first.close();
		dispatcher.dispatch(interactionEnvelope(8));
		assertFalse(first.active());
		assertEquals(List.of("first:toggle_open", "second:7", "second:8"), calls);
	}

	@Test
	void validatesPlatformNeutralReferencesAndSignals() {
		assertThrows(IllegalArgumentException.class, () -> new ActorRef("minecraft-player"));
		ObjectRef target = new ObjectRef(ArchitecturalObjectId.random());
		assertThrows(IllegalArgumentException.class, () -> new RedstoneSignalChangedEvent(target, 0, 16));
		assertThrows(IllegalArgumentException.class,
			() -> EventType.of("player_interact", PlayerInteractEvent.class));
	}

	private static EventEnvelope<PlayerInteractEvent> interactionEnvelope(long sequence) {
		WorldRef world = new WorldRef("minecraft:overworld");
		ObjectRef target = new ObjectRef(ArchitecturalObjectId.random());
		PlayerInteractEvent event = new PlayerInteractEvent(
			target,
			"toggle_open",
			new SpatialRef(world, Vec3d.ZERO, "door_panel")
		);
		EventContext context = new EventContext(
			world,
			new ActorRef("minecraft:player/test"),
			target,
			UUID.randomUUID(),
			null,
			Map.of()
		);
		return EventEnvelope.create(StandardEventTypes.PLAYER_INTERACT, event, context, Instant.EPOCH, sequence);
	}
}
