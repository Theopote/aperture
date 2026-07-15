package dev.aperture.api.material;

import dev.aperture.render.material.MaterialInstance;

/**
 * Resolves a named material slot to a platform-agnostic {@link MaterialInstance}.
 */
@FunctionalInterface
public interface MaterialResolver {
	MaterialInstance resolve(MaterialResolveContext context);
}
