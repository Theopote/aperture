package dev.aperture.runtime.event;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Synchronous ordered event bus; platform adapters can bridge it to their own buses. */
public final class RuntimeEventBus {
	private final CopyOnWriteArrayList<Consumer<RuntimeEvent>> subscribers = new CopyOnWriteArrayList<>();

	public AutoCloseable subscribe(Consumer<RuntimeEvent> subscriber) {
		Objects.requireNonNull(subscriber, "subscriber");
		subscribers.add(subscriber);
		return () -> subscribers.remove(subscriber);
	}

	public void publish(RuntimeEvent event) {
		Objects.requireNonNull(event, "event");
		for (Consumer<RuntimeEvent> subscriber : subscribers) {
			subscriber.accept(event);
		}
	}
}
