package dev.aperture.runtime.model.replication;

/** Explicit network allow-list for public runtime events. */
public sealed interface ReplicatedEvent permits CommandCommittedReplicationEvent,
	StateTransitionReplicationEvent {
	String eventType();
}
