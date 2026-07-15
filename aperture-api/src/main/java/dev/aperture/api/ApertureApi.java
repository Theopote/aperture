package dev.aperture.api;

import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.core.catalog.OpeningTypeRegistry;

/**
 * Public API surface for Aperture addon mods.
 */
public final class ApertureApi {
	private static ApertureApi instance;

	private final OpeningTypeRegistry openingTypes;
	private final GeneratorRegistry generators;

	public ApertureApi(OpeningTypeRegistry openingTypes, GeneratorRegistry generators) {
		this.openingTypes = openingTypes;
		this.generators = generators;
	}

	public static void init(ApertureApi api) {
		instance = api;
	}

	public static ApertureApi get() {
		if (instance == null) {
			throw new IllegalStateException("ApertureApi has not been initialized");
		}
		return instance;
	}

	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
	}

	public GeneratorRegistry generators() {
		return generators;
	}
}
