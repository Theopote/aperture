package dev.aperture.core.instance;

/**
 * Binding between a placed opening and its host building fabric.
 */
public record HostBinding(HostType type, String anchor) {
	public static HostBinding freeStanding() {
		return new HostBinding(HostType.FREE_STANDING, "");
	}

	public static HostBinding wall(String anchor) {
		return new HostBinding(HostType.WALL, anchor);
	}
}
