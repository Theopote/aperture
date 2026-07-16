package dev.aperture.core.editor.history.commands;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.history.EditCommand;
import dev.aperture.core.editor.history.EditResult;
import dev.aperture.core.editor.session.EditorContext;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parametric.ParametricEditor;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationResult;

import java.util.Map;
import java.util.Objects;

public final class SetParameterCommand implements EditCommand {
	private final EditorObjectId objectId;
	private final String parameterName;
	private final ParameterValue before;
	private final ParameterValue after;
	private final String description;

	public SetParameterCommand(
		EditorObjectId objectId,
		String parameterName,
		ParameterValue before,
		ParameterValue after,
		String description
	) {
		this.objectId = Objects.requireNonNull(objectId, "objectId");
		this.parameterName = Objects.requireNonNull(parameterName, "parameterName");
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

	private EditResult apply(EditorContext context, ParameterValue value) {
		EditorObject object = context.require(objectId);
		ParametricEditor editor = ParametricEditor.fromInstance(object.definition(), object.instance());
		var patch = editor.patch(Map.of(parameterName, dev.aperture.core.parametric.ParameterBridge.toExternalValue(value)));
		if (!patch.success()) {
			return EditResult.failed(description, patch.issues());
		}
		ValidationResult validation = editor.validate(object.definition());
		if (!validation.isValid()) {
			return EditResult.failed(description, validation.issues());
		}
		OpeningInstance next = object.instance()
			.withParameters(editor.overridesOnly())
			.withRevision(object.instance().revision() + 1);
		context.replaceObject(EditorObject.create(object.definition(), next));
		return EditResult.ok(description);
	}

	public static SetParameterCommand resizeLength(
		EditorObject object,
		String parameterName,
		double beforeMm,
		double afterMm
	) {
		ParameterSet resolved = object.definition().resolveParameters(object.instance().parameters());
		ParameterValue before = resolved.get(parameterName).orElse(ParameterValue.length(beforeMm));
		return new SetParameterCommand(
			object.id(),
			parameterName,
			before,
			ParameterValue.length(afterMm),
			"Resize " + parameterName + " to " + afterMm
		);
	}
}
