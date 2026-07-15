package dev.aperture.core.editor.history.commands;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.history.EditCommand;
import dev.aperture.core.editor.history.EditResult;
import dev.aperture.core.editor.session.EditorContext;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationResult;

import java.util.Objects;

public final class SetTransformCommand implements EditCommand {
	private final EditorObjectId objectId;
	private final Transform3d before;
	private final Transform3d after;
	private final String description;

	public SetTransformCommand(
		EditorObjectId objectId,
		Transform3d before,
		Transform3d after,
		String description
	) {
		this.objectId = Objects.requireNonNull(objectId, "objectId");
		this.before = Objects.requireNonNull(before, "before");
		this.after = Objects.requireNonNull(after, "after");
		this.description = Objects.requireNonNull(description, "description");
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public EditResult execute(EditorContext context) {
		return apply(context, after);
	}

	@Override
	public EditResult undo(EditorContext context) {
		return apply(context, before);
	}

	private EditResult apply(EditorContext context, Transform3d transform) {
		EditorObject object = context.require(objectId);
		OpeningInstance next = object.instance()
			.withTransform(transform)
			.withRevision(object.instance().revision() + 1);
		ValidationResult validation = context.validator().validate(object.definition(), next);
		if (!validation.isValid()) {
			return EditResult.failed(description, validation.issues());
		}
		context.replaceObject(EditorObject.create(object.definition(), next));
		return EditResult.ok(description);
	}
}
