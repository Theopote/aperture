package dev.aperture.opening.editor.plugin;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.plugin.ArchitecturalEditorPlugin;
import dev.aperture.opening.runtime.DoorRuntimeDefinition;
import dev.aperture.parameter.ParameterType;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Opening-family ownership of world editor semantics. */
public final class OpeningArchitecturalEditorPlugin implements ArchitecturalEditorPlugin {
	@Override public String id() { return "aperture:opening-editor"; }
	@Override public boolean supports(ObjectEditorView view) { return DoorRuntimeDefinition.OPENING_FAMILY.equals(view.familyId()); }

	@Override
	public List<ManipulatorDescriptor> manipulators(ObjectEditorView view, InspectorModel inspector) {
		var result = new ArrayList<ManipulatorDescriptor>();
		addLength(view, inspector, result, "door.width.right", "Width", "width",
			ManipulatorDescriptor.Axis.LOCAL_X, ManipulatorDescriptor.Anchor.RIGHT_MIDPOINT,
			ManipulatorDescriptor.Anchor.LEFT_MIDPOINT);
		addLength(view, inspector, result, "door.height.top", "Height", "height",
			ManipulatorDescriptor.Axis.LOCAL_Y, ManipulatorDescriptor.Anchor.TOP_MIDPOINT,
			ManipulatorDescriptor.Anchor.BOTTOM_MIDPOINT);
		return List.copyOf(result);
	}

	private static void addLength(ObjectEditorView view, InspectorModel inspector,
		List<ManipulatorDescriptor> target, String id, String label, String parameter,
		ManipulatorDescriptor.Axis axis, ManipulatorDescriptor.Anchor anchor,
		ManipulatorDescriptor.Anchor fixedAnchor) {
		if (view.parameters().get(parameter).filter(value -> value.type() == ParameterType.LENGTH).isEmpty()) return;
		OptionalDouble minimum = OptionalDouble.empty();
		OptionalDouble maximum = OptionalDouble.empty();
		for (var section : inspector.sections(view.objectId())) for (var property : section.properties()) {
			if (property.key().equals(parameter)) { minimum = property.minimum(); maximum = property.maximum(); }
		}
		target.add(new ManipulatorDescriptor(id, ManipulatorDescriptor.Kind.LINEAR_PARAMETER, label, parameter,
			axis, anchor, fixedAnchor, ManipulatorDescriptor.DirectionPolicy.POSITIVE,
			minimum, maximum, 10.0, 1.0, ParameterType.LENGTH));
	}
}
