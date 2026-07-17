package dev.aperture.runtime.model.event;

public final class StandardEventTypes {
	public static final EventType<PlayerInteractEvent> PLAYER_INTERACT = type("player_interact", PlayerInteractEvent.class);
	public static final EventType<RedstoneSignalChangedEvent> REDSTONE_SIGNAL_CHANGED = type("redstone_signal_changed", RedstoneSignalChangedEvent.class);
	public static final EventType<TimeChangedEvent> TIME_CHANGED = type("time_changed", TimeChangedEvent.class);
	public static final EventType<WeatherChangedEvent> WEATHER_CHANGED = type("weather_changed", WeatherChangedEvent.class);
	public static final EventType<NeighborChangedEvent> NEIGHBOR_CHANGED = type("neighbor_changed", NeighborChangedEvent.class);
	public static final EventType<CommandAppliedEvent> COMMAND_APPLIED = type("command_applied", CommandAppliedEvent.class);
	public static final EventType<StateChangedEvent> STATE_CHANGED = type("state_changed", StateChangedEvent.class);
	public static final EventType<HostChangedEvent> HOST_CHANGED = type("host_changed", HostChangedEvent.class);
	public static final EventType<ObjectCreatedEvent> OBJECT_CREATED = type("object_created", ObjectCreatedEvent.class);
	public static final EventType<ObjectRemovedEvent> OBJECT_REMOVED = type("object_removed", ObjectRemovedEvent.class);

	private StandardEventTypes() { }

	private static <T extends ArchitecturalEvent> EventType<T> type(String path, Class<T> payloadType) {
		return EventType.of("aperture:" + path, payloadType);
	}
}
