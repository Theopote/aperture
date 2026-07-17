package dev.aperture.runtime.model.event;

import java.util.Objects;

/** Platform-neutral player, automation, service, or system identity. */
public record ActorRef(String id) {
	public static final ActorRef SYSTEM = new ActorRef("aperture:system");
	public ActorRef { id = References.requireNamespaced(id, "actor"); }
}
