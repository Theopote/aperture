package dev.aperture.runtime.material;

import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.core.material.MaterialDefinition;
import dev.aperture.core.material.MaterialInstance;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves material slots using the data-pack backed {@link MaterialCatalogRegistry}.
 */
public final class CatalogMaterialResolver implements MaterialResolver {
	private final MaterialCatalogRegistry catalog;

	public CatalogMaterialResolver(MaterialCatalogRegistry catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	@Override
	public MaterialInstance resolve(MaterialResolveContext context) {
		MaterialDefinition base = catalog.resolveSlot(context.materialSlot());
		Optional<String> overrideTexture = MaterialResolveSupport.materialRefOverride(context);
		MaterialDefinition resolved = overrideTexture
			.map(texture -> MaterialResolveSupport.withAlbedo(base, texture))
			.orElse(base);
		MaterialDefinition layerAdjusted = MaterialResolveSupport.adjustForLayer(resolved, context.layer());
		return MaterialInstance.of(layerAdjusted);
	}
}
