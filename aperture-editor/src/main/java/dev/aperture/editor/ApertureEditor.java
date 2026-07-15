package dev.aperture.editor;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.editor.service.EditorService;
import dev.aperture.editor.service.ParametricService;
import dev.aperture.runtime.ApertureRuntime;

/**
 * Client/editor facade: selection, gizmos, resize, inspector, history, and undo.
 * Depends on {@link ApertureRuntime} but is not required on dedicated servers.
 */
public final class ApertureEditor {
	private static ApertureEditor instance;

	private final EditorService editor;
	private final ParametricService parametrics;

	public ApertureEditor(EditorService editor, ParametricService parametrics) {
		this.editor = editor;
		this.parametrics = parametrics;
	}

	public static void init(ApertureEditor editor) {
		instance = editor;
	}

	public static ApertureEditor get() {
		if (instance == null) {
			throw new IllegalStateException("ApertureEditor has not been initialized");
		}
		return instance;
	}

	public static boolean isAvailable() {
		return instance != null;
	}

	public EditorService editor() {
		return editor;
	}

	public ParametricService parametrics() {
		return parametrics;
	}

	public OpeningTypeRegistry openingTypes() {
		return ApertureRuntime.get().openingTypes();
	}
}
