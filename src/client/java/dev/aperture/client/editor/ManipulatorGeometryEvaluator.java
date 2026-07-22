package dev.aperture.client.editor;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.math.Vec3d;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Evaluates declarative local manipulator geometry into Minecraft world space. */
public final class ManipulatorGeometryEvaluator {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;

	public Optional<EvaluatedManipulator> evaluate(ObjectEditorView view, ManipulatorDescriptor descriptor) {
		var opening = OpeningWorldGeometry.from(view);
		if (opening.isEmpty()) return Optional.empty();
		var geometry = opening.get();
		Vec3 handle = switch (descriptor.anchor()) {
			case RIGHT_MIDPOINT -> geometry.rightWidthHandle();
			case LEFT_MIDPOINT -> geometry.leftWidthHandle();
			default -> null;
		};
		Vec3 fixed = switch (descriptor.fixedAnchor()) {
			case LEFT_MIDPOINT -> geometry.leftWidthHandle();
			case RIGHT_MIDPOINT -> geometry.rightWidthHandle();
			default -> null;
		};
		if (handle == null || fixed == null) return Optional.empty();
		Vec3d localAxis = switch (descriptor.axis()) {
			case LOCAL_X -> new Vec3d(1, 0, 0);
			case LOCAL_Y -> new Vec3d(0, 1, 0);
			case LOCAL_Z -> new Vec3d(0, 0, 1);
		};
		Vec3d transformedOrigin = view.transform().transformPoint(Vec3d.ZERO);
		Vec3d transformedAxisPoint = view.transform().transformPoint(localAxis.scale(MILLIMETERS_PER_BLOCK));
		Vec3d worldAxis = transformedAxisPoint.subtract(transformedOrigin).normalize();
		if (descriptor.direction() == ManipulatorDescriptor.DirectionPolicy.NEGATIVE) worldAxis = worldAxis.scale(-1);
		return Optional.of(new EvaluatedManipulator(descriptor, handle, fixed,
			new Vec3(worldAxis.x(), worldAxis.y(), worldAxis.z())));
	}

	public record EvaluatedManipulator(ManipulatorDescriptor descriptor, Vec3 handle,
		Vec3 fixedAnchor, Vec3 worldAxis) { }
}
