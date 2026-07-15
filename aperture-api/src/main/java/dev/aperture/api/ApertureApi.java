package dev.aperture.api;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementService;
import dev.aperture.editor.ApertureEditor;
import dev.aperture.editor.service.EditorService;
import dev.aperture.editor.service.ParametricService;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.runtime.registry.GeneratorRegistry;
import dev.aperture.runtime.registry.MaterialResolverRegistry;
import dev.aperture.runtime.service.OpeningGenerationService;

/**
 * Combined facade for addon mods that need both runtime and editor surfaces.
 * Dedicated servers should use {@link ApertureRuntime} only.
 */
@Deprecated
public final class ApertureApi {
	private static ApertureApi instance;

	private final ApertureRuntime runtime;
	private final ApertureEditor editor;

	public ApertureApi(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		MaterialCatalogRegistry materialCatalog,
		MaterialResolverRegistry materials,
		OpeningInstanceStore instances,
		OpeningGenerationService generation,
		PlacementService placement
	) {
		this(
			new ApertureRuntime(openingTypes, generators, profiles, materialCatalog, materials, instances, generation, placement),
			new ApertureEditor(new EditorService(), new ParametricService())
		);
	}

	public ApertureApi(ApertureRuntime runtime, ApertureEditor editor) {
		this.runtime = runtime;
		this.editor = editor;
	}

	public static void init(ApertureApi api) {
		instance = api;
		ApertureRuntime.init(api.runtime);
		ApertureEditor.init(api.editor);
	}

	public static ApertureApi get() {
		if (instance == null) {
			throw new IllegalStateException("ApertureApi has not been initialized");
		}
		return instance;
	}

	public OpeningTypeRegistry openingTypes() {
		return runtime.openingTypes();
	}

	public GeneratorRegistry generators() {
		return runtime.generators();
	}

	public ProfileCatalogRegistry profiles() {
		return runtime.profiles();
	}

	public MaterialCatalogRegistry materialCatalog() {
		return runtime.materialCatalog();
	}

	public MaterialResolverRegistry materials() {
		return runtime.materials();
	}

	public OpeningInstanceStore instances() {
		return runtime.instances();
	}

	public OpeningGenerationService generation() {
		return runtime.generation();
	}

	public ParametricService parametrics() {
		return editor.parametrics();
	}

	public EditorService editor() {
		return editor.editor();
	}

	public PlacementService placement() {
		return runtime.placement();
	}

	public ApertureRuntime runtime() {
		return runtime;
	}

	public ApertureEditor editorFacade() {
		return editor;
	}
}
