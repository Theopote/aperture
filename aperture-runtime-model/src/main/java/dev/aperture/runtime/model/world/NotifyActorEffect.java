package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.ActorRef;
import java.util.Objects;

public record NotifyActorEffect(ActorRef actor, String messageKey) implements WorldEffect {
	public NotifyActorEffect { Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(messageKey, "messageKey"); }
	@Override public String effectType() { return "aperture:notify_actor"; }
}
