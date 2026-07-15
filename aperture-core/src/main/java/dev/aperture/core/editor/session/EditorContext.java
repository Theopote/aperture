package dev.aperture.core.editor.session;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.validation.OpeningValidator;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable state shared by edit commands during execute / undo.
 */
public final class EditorContext {
	private final Map<EditorObjectId, EditorObject> objects;
	private final OpeningTypeRegistry openingTypes;
	private final OpeningValidator validator;

	public EditorContext(
		Map<EditorObjectId, EditorObject> objects,
		OpeningTypeRegistry openingTypes,
		OpeningValidator validator
	) {
		this.objects = objects;
		this.openingTypes = Objects.requireNonNull(openingTypes, "openingTypes");
		this.validator = Objects.requireNonNull(validator, "validator");
	}

	public Optional<EditorObject> object(EditorObjectId id) {
		return Optional.ofNullable(objects.get(id));
	}

	public EditorObject require(EditorObjectId id) {
		return object(id).orElseThrow(() -> new IllegalArgumentException("Unknown editor object: " + id));
	}

	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
	}

	public OpeningValidator validator() {
		return validator;
	}

	public void replaceObject(EditorObject object) {
		objects.put(object.id(), object);
	}

	public void addObject(EditorObject object) {
		objects.put(object.id(), object);
	}

	public void removeObject(EditorObjectId id) {
		objects.remove(id);
	}
}
