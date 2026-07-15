package dev.aperture.runtime.material;

import dev.aperture.core.material.MaterialInstance;

/**
 * Resolves a named material slot to a platform-agnostic {@link MaterialInstance}.
 */
@FunctionalInterface
public interface MaterialResolver {
	MaterialInstance resolve(MaterialResolveContext context);
}
