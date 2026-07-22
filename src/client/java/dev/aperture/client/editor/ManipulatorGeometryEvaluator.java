package dev.aperture.client.editor;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.math.Vec3d;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Evaluates declarative local manipulator geometry into Minecraft world space. */
public final class ManipulatorGeometryEvaluator {

	public Optional<EvaluatedManipulator> evaluate(ObjectEditorView view, ManipulatorDescriptor descriptor) {
		var opening = OpeningWorldGeometry.from(view);
		if (opening.isEmpty()) return Optional.empty();
		var geometry = opening.get();
		Vec3 handle = switch (descriptor.anchor()) {
			case RIGHT_MIDPOINT -> geometry.rightWidthHandle();
			case LEFT_MIDPOINT -> geometry.leftWidthHandle();
			case TOP_MIDPOINT -> geometry.topHeightHandle();
			case BOTTOM_MIDPOINT -> geometry.bottomHeightHandle();
			default -> null;
		};
		Vec3 fixed = switch (descriptor.fixedAnchor()) {
			case LEFT_MIDPOINT -> geometry.leftWidthHandle();
			case RIGHT_MIDPOINT -> geometry.rightWidthHandle();
			case TOP_MIDPOINT -> geometry.topHeightHandle();
			case BOTTOM_MIDPOINT -> geometry.bottomHeightHandle();
			default -> null;
		};
		if (handle == null || fixed == null) return Optional.empty();
		Vec3d localAxis = switch (descriptor.axis()) {
			case LOCAL_X -> new Vec3d(1, 0, 0);
			case LOCAL_Y -> new Vec3d(0, 1, 0);
			case LOCAL_Z -> new Vec3d(0, 0, 1);
		};
		Vec3d transformedOrigin = view.transform().transformPoint(Vec3d.ZERO);
		Vec3d transformedAxisPoint = view.transform().transformPoint(localAxis.scale(dev.aperture.editor.interaction.WorldUnitConverter.MILLIMETERS_PER_BLOCK));
		Vec3d worldAxis = transformedAxisPoint.subtract(transformedOrigin).normalize();
		Vec3 label = switch (descriptor.axis()) {
			case LOCAL_X -> geometry.dimensionLabel();
			case LOCAL_Y -> geometry.heightDimensionLabel();
			case LOCAL_Z -> handle;
		};
		return Optional.of(new EvaluatedManipulator(descriptor, handle, fixed, label,
			new Vec3(worldAxis.x(), worldAxis.y(), worldAxis.z())));
	}

	public record EvaluatedManipulator(ManipulatorDescriptor descriptor, Vec3 handle,
		Vec3 fixedAnchor, Vec3 dimensionLabel, Vec3 worldAxis) { }
}
