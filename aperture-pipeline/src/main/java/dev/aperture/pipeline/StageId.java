package dev.aperture.pipeline;

import java.util.Arrays;

/** Stable identifier for a pipeline stage. */
public enum StageId {
	DEFINITION("definition"),
	PARAMETER("parameter"),
	CONSTRAINT("constraint"),
	COMPONENT("component"),
	GEOMETRY("geometry"),
	MESH("mesh"),
	COLLISION("collision"),
	PLACEMENT("placement"),
	CUSTOM("custom");

	private final String externalName;

	StageId(String externalName) {
		this.externalName = externalName;
	}

	public String externalName() {
		return externalName;
	}

	public static StageId fromExternalName(String name) {
		return Arrays.stream(values())
			.filter(value -> value.externalName.equals(name))
			.findFirst()
			.orElse(CUSTOM);
	}
}