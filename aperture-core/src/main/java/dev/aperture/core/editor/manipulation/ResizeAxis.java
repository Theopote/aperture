package dev.aperture.core.editor.manipulation;

/**
 * Which parametric dimension a resize handle drives.
 */
public enum ResizeAxis {
	WIDTH("width"),
	HEIGHT("height"),
	THICKNESS("thickness", "frame_depth", "frame_width");

	private final String[] parameterNames;

	ResizeAxis(String... parameterNames) {
		this.parameterNames = parameterNames;
	}

	public String[] parameterNames() {
		return parameterNames;
	}
}
