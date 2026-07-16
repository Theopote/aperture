package dev.aperture.kernel.internal;

import dev.aperture.core.registry.OpeningTypeRegistry;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Internal configuration for ApertureKernel.
 * <p>
 * Package-private, not part of public API.
 */
public record KernelConfig(
	OpeningTypeRegistry registry,
	OpeningPipelineAdapter pipeline,
	ProfileCatalogRegistry profiles,
	ExecutorService executorService,
	boolean enableDebugLogging
) {
	public KernelConfig {
		Objects.requireNonNull(registry, "registry cannot be null");
		Objects.requireNonNull(pipeline, "pipeline cannot be null");
		Objects.requireNonNull(profiles, "profiles cannot be null");
		Objects.requireNonNull(executorService, "executorService cannot be null");
	}
}
