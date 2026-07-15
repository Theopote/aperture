package dev.aperture.core.instance;

/**
 * Type of structural host an opening attaches to.
 */
public enum HostType {
	WALL,
	ROOF,
	CURTAIN_WALL_HOST,
	FREE_STANDING;

	public String id() {
		return name().toLowerCase();
	}

	public static HostType fromId(String id) {
		return valueOf(id.toUpperCase());
	}
}
