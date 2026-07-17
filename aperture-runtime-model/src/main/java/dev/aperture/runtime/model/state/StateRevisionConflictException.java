package dev.aperture.runtime.model.state;

public final class StateRevisionConflictException extends IllegalStateException {
	public StateRevisionConflictException(StateRevision expected, StateRevision actual) {
		super("State revision conflict: expected " + expected.value() + " but was " + actual.value());
	}
}
