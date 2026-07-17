package dev.aperture.runtime.model.behavior;

/** Executable behavior bound to an immutable definition. */
public interface BehaviorInstance {
	BehaviorDefinition definition();
	BehaviorResult evaluate(BehaviorContext context);
}
