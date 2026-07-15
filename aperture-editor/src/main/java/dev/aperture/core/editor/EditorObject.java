package dev.aperture.core.editor;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.editor.constraint.EditorConstraint;
import dev.aperture.core.editor.manipulation.EditorManipulatorFactory;
import dev.aperture.core.editor.manipulation.Manipulator;
import dev.aperture.core.editor.manipulation.ResizeHandle;
import dev.aperture.core.editor.snap.SnapPoint;
import dev.aperture.core.instance.OpeningInstance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Session-scoped editable view of one opening instance.
 * GUI, AI, and NodeCraft operate on {@link EditorObject} — not on generator internals.
 */
public final class EditorObject {
	private final EditorObjectId id;
	private final OpeningTypeDefinition definition;
	private OpeningInstance instance;
	private final EditorManipulatorFactory.ManipulationLayout manipulation;
	private final List<SnapPoint> snapPoints;
	private final List<EditorConstraint> constraints;

	private EditorObject(
		EditorObjectId id,
		OpeningTypeDefinition definition,
		OpeningInstance instance,
		EditorManipulatorFactory.ManipulationLayout manipulation,
		List<SnapPoint> snapPoints,
		List<EditorConstraint> constraints
	) {
		this.id = id;
		this.definition = definition;
		this.instance = instance;
		this.manipulation = manipulation;
		this.snapPoints = List.copyOf(snapPoints);
		this.constraints = List.copyOf(constraints);
	}

	public static EditorObject create(OpeningTypeDefinition definition, OpeningInstance instance) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(instance, "instance");
		EditorManipulatorFactory.ManipulationLayout layout = EditorManipulatorFactory.layout(definition, instance);
		return new EditorObject(
			EditorObjectId.fromInstanceId(instance.instanceId()),
			definition,
			instance,
			layout,
			List.of(),
			List.of()
		);
	}

	public EditorObjectId id() {
		return id;
	}

	public OpeningTypeDefinition definition() {
		return definition;
	}

	public OpeningInstance instance() {
		return instance;
	}

	public List<Manipulator> manipulators() {
		return manipulation.manipulators();
	}

	public List<ResizeHandle> resizeHandles() {
		return manipulation.resizeHandles();
	}

	public Optional<ResizeHandle> resizeHandle(String handleId) {
		return Optional.ofNullable(manipulation.resizeHandlesById().get(handleId));
	}

	public List<SnapPoint> snapPoints() {
		return snapPoints;
	}

	public List<EditorConstraint> constraints() {
		return constraints;
	}

	void replaceInstance(OpeningInstance next) {
		this.instance = Objects.requireNonNull(next, "next");
	}
}
