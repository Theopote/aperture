package dev.aperture.runtime.model.world;

import java.util.Objects;

public record WorldEffectResult(Status status, String message) {
	public enum Status { APPLIED, REJECTED, FAILED }
	public WorldEffectResult { Objects.requireNonNull(status, "status"); Objects.requireNonNull(message, "message"); }
	public static WorldEffectResult applied() { return new WorldEffectResult(Status.APPLIED, ""); }
}
