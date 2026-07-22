package dev.aperture.client.editor;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.interaction.ManipulatorDescriptorProvider;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.parameter.ParameterType;

import java.util.List;
import java.util.OptionalDouble;

/** Opening-family editor projection. The tool remains independent of parameter names and local axes. */
public final class OpeningManipulatorDescriptorProvider implements ManipulatorDescriptorProvider {
	private final InspectorModel inspector;

	public OpeningManipulatorDescriptorProvider(InspectorModel inspector) { this.inspector = inspector; }

	@Override
	public List<ManipulatorDescriptor> descriptors(ObjectEditorView view) {
		if (view.parameters().get("width").filter(value -> value.type() == ParameterType.LENGTH).isEmpty()) return List.of();
		OptionalDouble minimum = OptionalDouble.empty();
		OptionalDouble maximum = OptionalDouble.empty();
		for (var section : inspector.sections(view.objectId())) for (var property : section.properties()) {
			if (property.key().equals("width")) {
				minimum = property.minimum();
				maximum = property.maximum();
			}
		}
		return List.of(new ManipulatorDescriptor("door.width.right", ManipulatorDescriptor.Kind.LINEAR_PARAMETER,
			"Width", "width", ManipulatorDescriptor.Axis.LOCAL_X,
			ManipulatorDescriptor.Anchor.RIGHT_MIDPOINT, ManipulatorDescriptor.Anchor.LEFT_MIDPOINT,
			ManipulatorDescriptor.DirectionPolicy.POSITIVE, minimum, maximum, 10.0, 1.0, ParameterType.LENGTH));
	}
}
