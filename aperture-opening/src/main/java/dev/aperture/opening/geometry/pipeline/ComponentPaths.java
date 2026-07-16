package dev.aperture.opening.geometry.pipeline;

/**
 * Builds geometry component paths rooted at a component instance id.
 */
public final class ComponentPaths {
	private ComponentPaths() {
	}

	public static String join(String componentId, String suffix) {
		return componentId + "." + suffix;
	}
}
