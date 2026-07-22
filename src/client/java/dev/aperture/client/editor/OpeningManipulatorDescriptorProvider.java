package dev.aperture.client.editor;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.interaction.ManipulatorDescriptorProvider;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.parameter.ParameterType;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Opening-family editor projection. The tool remains independent of parameter names and local axes. */
public final class OpeningManipulatorDescriptorProvider implements ManipulatorDescriptorProvider {
	private final InspectorModel inspector;

	public OpeningManipulatorDescriptorProvider(InspectorModel inspector) { this.inspector = inspector; }

	@Override
	public List<ManipulatorDescriptor> descriptors(ObjectEditorView view) {
		var result = new ArrayList<ManipulatorDescriptor>();
		addLength(view, result, "door.width.right", "Width", "width",
			ManipulatorDescriptor.Axis.LOCAL_X, ManipulatorDescriptor.Anchor.RIGHT_MIDPOINT,
			ManipulatorDescriptor.Anchor.LEFT_MIDPOINT);
		addLength(view, result, "door.height.top", "Height", "height",
			ManipulatorDescriptor.Axis.LOCAL_Y, ManipulatorDescriptor.Anchor.TOP_MIDPOINT,
			ManipulatorDescriptor.Anchor.BOTTOM_MIDPOINT);
		return List.copyOf(result);
	}

	private void addLength(ObjectEditorView view, List<ManipulatorDescriptor> target, String id, String label,
		String parameter, ManipulatorDescriptor.Axis axis, ManipulatorDescriptor.Anchor anchor,
		ManipulatorDescriptor.Anchor fixedAnchor) {
		if (view.parameters().get(parameter).filter(value -> value.type() == ParameterType.LENGTH).isEmpty()) return;
		OptionalDouble minimum = OptionalDouble.empty();
		OptionalDouble maximum = OptionalDouble.empty();
		for (var section : inspector.sections(view.objectId())) for (var property : section.properties()) {
			if (property.key().equals(parameter)) {
				minimum = property.minimum();
				maximum = property.maximum();
			}
		}
		target.add(new ManipulatorDescriptor(id, ManipulatorDescriptor.Kind.LINEAR_PARAMETER, label, parameter,
			axis, anchor, fixedAnchor, ManipulatorDescriptor.DirectionPolicy.POSITIVE,
			minimum, maximum, 10.0, 1.0, ParameterType.LENGTH));
	}
}