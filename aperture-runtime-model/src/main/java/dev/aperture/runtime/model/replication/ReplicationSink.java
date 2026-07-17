package dev.aperture.runtime.model.replication;

/** Outbound port used by the authoritative runtime after a committed transition. */
@FunctionalInterface
public interface ReplicationSink {
	void publish(ReplicationMessage message);

	static ReplicationSink noop() { return message -> { }; }
}
