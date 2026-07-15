package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.component.OpeningComponent;

/**
 * Geometry step bound to one {@link OpeningComponent} instance.
 * Step identity and geometry paths trace back to {@link OpeningComponent#ref() id}.
 */
public interface ComponentPipelineStep extends PipelineStep {
	OpeningComponent component();

	@Override
	default String id() {
		return component().ref().id();
	}
}
