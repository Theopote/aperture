package dev.aperture.core.component;

/**
 * Canonical opening component roles. Opening types (window, door, curtain wall)
 * are defined as different combinations of these components — not separate generators.
 */
public enum ComponentKind {
	FRAME("frame"),
	PANEL("panel"),
	GLASS("glass"),
	MULLION("mullion"),
	DIVIDER("divider"),
	HARDWARE("hardware"),
	HANDLE("handle"),
	TRIM("trim"),
	SILL("sill"),
	HEADER("header"),
	DECORATION("decoration");

	private final String jsonKey;

	ComponentKind(String jsonKey) {
		this.jsonKey = jsonKey;
	}

	public String jsonKey() {
		return jsonKey;
	}

	public static ComponentKind fromJsonKey(String key) {
		for (ComponentKind kind : values()) {
			if (kind.jsonKey.equalsIgnoreCase(key)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Unknown component kind: " + key);
	}
}
