package dev.aperture.runtime.model.event;

public interface EventSubscription extends AutoCloseable {
	boolean active();
	@Override void close();
}
