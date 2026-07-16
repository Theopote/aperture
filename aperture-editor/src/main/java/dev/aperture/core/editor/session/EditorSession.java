package dev.aperture.core.editor.session;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.Selection;
import dev.aperture.core.editor.history.EditCommand;
import dev.aperture.core.editor.history.EditHistory;
import dev.aperture.core.editor.history.EditResult;
import dev.aperture.core.editor.history.commands.AddObjectCommand;
import dev.aperture.core.editor.history.commands.SetParameterCommand;
import dev.aperture.core.editor.history.commands.SetTransformCommand;
import dev.aperture.core.editor.manipulation.MirrorAxis;
import dev.aperture.core.editor.manipulation.ResizeAxis;
import dev.aperture.core.editor.operation.EditorOperations;
import dev.aperture.core.editor.snap.SnapEngine;
import dev.aperture.core.editor.snap.SnapPolicy;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.CompositeOpeningValidator;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ParameterConstraintValidator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Headless editor session. All interactive tools (GUI, AI, NodeCraft) operate through this API.
 */
public final class EditorSession {
	private final OpeningTypeRegistry openingTypes;
	private final OpeningValidator validator;
	private final EditHistory history = new EditHistory();
	private final SnapEngine snapEngine = new SnapEngine();
	private final SnapPolicy snapPolicy;
	private final Map<EditorObjectId, EditorObject> objects = new LinkedHashMap<>();
	private final EditorContext context;
	private Selection selection = Selection.empty();

	public EditorSession(OpeningTypeRegistry openingTypes) {
		this(openingTypes, CompositeOpeningValidator.schemaAndConstraints(new ParameterConstraintValidator()), SnapPolicy.defaults());
	}

	public EditorSession(OpeningTypeRegistry openingTypes, OpeningValidator validator, SnapPolicy snapPolicy) {
		this.openingTypes = Objects.requireNonNull(openingTypes, "openingTypes");
		this.validator = Objects.requireNonNull(validator, "validator");
		this.snapPolicy = Objects.requireNonNull(snapPolicy, "snapPolicy");
		this.context = new EditorContext(objects, openingTypes, validator);
	}

	public EditorObject addObject(OpeningInstance instance) {
		OpeningTypeDefinition definition = openingTypes.require(instance.typeId());
		EditorObject object = EditorObject.create(definition, instance);
		objects.put(object.id(), object);
		return object;
	}

	public Optional<EditorObject> object(EditorObjectId id) {
		return Optional.ofNullable(objects.get(id));
	}

	public Map<EditorObjectId, EditorObject> objects() {
		return Map.copyOf(objects);
	}

	public Selection selection() {
		return selection;
	}

	public EditorSession select(EditorObjectId id) {
		if (!objects.containsKey(id)) {
			throw new IllegalArgumentException("Unknown editor object: " + id);
		}
		selection.select(id);
		return this;
	}

	public EditHistory history() {
		return history;
	}

	public EditResult execute(EditCommand command) {
		return history.execute(command, context);
	}

	public EditResult undo() {
		return history.undo(context);
	}

	public EditResult redo() {
		return history.redo(context);
	}

	public EditResult resizeByHandle(EditorObjectId id, String handleId, double deltaMm) {
		EditorObject object = require(id);
		var handle = object.resizeHandle(handleId)
			.orElseThrow(() -> new IllegalArgumentException("Unknown resize handle: " + handleId));
		double current = EditorOperations.currentLength(object, handle.parameterName());
		double next = EditorOperations.clampLength(object, handle.parameterName(), current + deltaMm);
		return execute(SetParameterCommand.resizeLength(object, handle.parameterName(), current, next));
	}

	public EditResult resizeAxis(EditorObjectId id, ResizeAxis axis, double deltaMm) {
		EditorObject object = require(id);
		var handle = EditorOperations.handleForAxis(object, axis)
			.orElseThrow(() -> new IllegalArgumentException("Opening type has no " + axis + " parameter"));
		double current = EditorOperations.currentLength(object, handle.parameterName());
		double next = EditorOperations.clampLength(object, handle.parameterName(), current + deltaMm);
		return execute(SetParameterCommand.resizeLength(object, handle.parameterName(), current, next));
	}

	public EditResult rotate(EditorObjectId id, double degrees) {
		EditorObject object = require(id);
		Transform3d before = object.instance().transform();
		Transform3d after = EditorOperations.rotateTransform(before, degrees);
		return execute(new SetTransformCommand(object.id(), before, after, "Rotate " + degrees + "°"));
	}

	public EditResult mirror(EditorObjectId id, MirrorAxis axis) {
		EditorObject object = require(id);
		Transform3d before = object.instance().transform();
		Transform3d after = EditorOperations.mirrorTransform(before, axis);
		EditResult transformResult = execute(new SetTransformCommand(object.id(), before, after, "Mirror " + axis));
		if (!transformResult.success()) {
			return transformResult;
		}
		Optional<ParameterValue> mirroredHinge = EditorOperations.mirroredHingeSide(require(id));
		if (mirroredHinge.isEmpty()) {
			return transformResult;
		}
		EditorObject updated = require(id);
		ParameterValue beforeHinge = EditorOperations.parametricEditor(updated).resolved("hinge_side");
		return execute(new SetParameterCommand(
			id,
			"hinge_side",
			beforeHinge,
			mirroredHinge.get(),
			"Mirror hinge side"
		));
	}

	public EditResult copy(EditorObjectId id, Vec3d offset) {
		EditorObject source = require(id);
		OpeningInstance duplicate = EditorOperations.duplicateInstance(source, offset);
		EditResult result = execute(new AddObjectCommand(EditorObject.create(source.definition(), duplicate)));
		if (result.success()) {
			selection.add(EditorObjectId.fromInstanceId(duplicate.instanceId()));
		}
		return result;
	}

	public Vec3d snapDragPoint(EditorObjectId id, Vec3d candidate) {
		EditorObject object = require(id);
		return snapEngine.snap(candidate, object.snapPoints(), snapPolicy);
	}

	private EditorObject require(EditorObjectId id) {
		return object(id).orElseThrow(() -> new IllegalArgumentException("Unknown editor object: " + id));
	}
}
