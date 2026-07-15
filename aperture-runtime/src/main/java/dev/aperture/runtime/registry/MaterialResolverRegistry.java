package dev.aperture.runtime.registry;

import dev.aperture.runtime.material.MaterialResolveContext;
import dev.aperture.runtime.material.MaterialResolver;
import dev.aperture.render.material.MaterialInstance;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of material resolvers keyed by slot name, with a fallback resolver.
 */
public final class MaterialResolverRegistry {
	private final Map<String, MaterialResolver> bySlot = new ConcurrentHashMap<>();
	private MaterialResolver fallback;

	public MaterialResolverRegistry(MaterialResolver fallback) {
		this.fallback = Objects.requireNonNull(fallback, "fallback");
	}

	public void register(String slot, MaterialResolver resolver) {
		Objects.requireNonNull(slot, "slot");
		Objects.requireNonNull(resolver, "resolver");
		bySlot.put(slot, resolver);
	}

	public void setFallback(MaterialResolver fallback) {
		this.fallback = Objects.requireNonNull(fallback, "fallback");
	}

	public MaterialInstance resolve(MaterialResolveContext context) {
		MaterialResolver resolver = bySlot.getOrDefault(context.materialSlot(), fallback);
		return resolver.resolve(context);
	}
}
