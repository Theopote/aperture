package dev.aperture.core.opening;

/**
 * High-level classification of an opening. Categories influence behavior
 * strategies but do not define separate domain types.
 */
public enum OpeningCategory {
	DOOR("door"),
	WINDOW("window"),
	CURTAIN_WALL("curtain_wall"),
	SKYLIGHT("skylight"),
	FACADE("facade");

	private final String id;

	OpeningCategory(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static OpeningCategory fromId(String id) {
		for (OpeningCategory category : values()) {
			if (category.id.equals(id)) {
				return category;
			}
		}
		throw new IllegalArgumentException("Unknown opening category: " + id);
	}
}
