package dev.aperture.core.editor.history.commands;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.history.EditCommand;
import dev.aperture.core.editor.history.EditResult;
import dev.aperture.core.editor.session.EditorContext;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationResult;

import java.util.Objects;

public final class AddObjectCommand implements EditCommand {
	private final EditorObjectId objectId;
	private final OpeningInstance instance;

	public AddObjectCommand(EditorObject object) {
		this.objectId = object.id();
		this.instance = object.instance();
	}

	@Override
	public String description() {
		return "Add opening";
	}

	@Override
	public EditResult execute(EditorContext context) {
		var definition = context.openingTypes().require(instance.typeId());
		ValidationResult validation = context.validator().validate(definition, instance);
		if (!validation.isValid()) {
			return EditResult.failed(description(), validation.issues());
		}
		context.addObject(EditorObject.create(definition, instance));
		return EditResult.ok(description());
	}

	@Override
	public EditResult undo(EditorContext context) {
		if (context.object(objectId).isEmpty()) {
			return EditResult.failed(description(), "editor.missing_object", "Object already removed");
		}
		context.removeObject(objectId);
		return EditResult.ok("Remove opening");
	}
}
