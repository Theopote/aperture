package dev.aperture.client.editor;

import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Parameter-derived world presentation geometry for an opening editor view. */
public final class OpeningWorldGeometry {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;

	private OpeningWorldGeometry() { }

	public static Optional<Presentation> from(ObjectEditorView view) {
		double width = length(view, "width").orElse(-1.0);
		double height = length(view, "height").orElse(-1.0);
		if (width <= 0 || height <= 0) return Optional.empty();
		double depth = thickness(view).orElse(100.0);
		BoundingBox world = view.transform().applyTo(new BoundingBox(Vec3d.ZERO, new Vec3d(width, height, depth)));
		Vec3 leftHandle = blocks(view.transform().transformPoint(new Vec3d(0, height / 2.0, depth / 2.0)));
		Vec3 rightHandle = blocks(view.transform().transformPoint(new Vec3d(width, height / 2.0, depth / 2.0)));
		Vec3 bottomHandle = blocks(view.transform().transformPoint(new Vec3d(width / 2.0, 0, depth / 2.0)));
		Vec3 topHandle = blocks(view.transform().transformPoint(new Vec3d(width / 2.0, height, depth / 2.0)));
		Vec3 heightLabel = blocks(view.transform().transformPoint(new Vec3d(width + 150.0, height / 2.0, depth / 2.0)));
		double dimensionY = height + 150.0;
		Vec3 dimensionStart = blocks(view.transform().transformPoint(new Vec3d(0, dimensionY, depth / 2.0)));
		Vec3 dimensionEnd = blocks(view.transform().transformPoint(new Vec3d(width, dimensionY, depth / 2.0)));
		return Optional.of(new Presentation(width, height, depth, aabb(world), leftHandle, rightHandle,
			bottomHandle, topHandle, heightLabel, dimensionStart, dimensionEnd, dimensionStart.add(dimensionEnd).scale(.5)));
	}

	private static Optional<Double> thickness(ObjectEditorView view) {
		for (String key : new String[]{"thickness", "frame_depth", "frame_width", "depth"}) {
			Optional<Double> value = length(view, key);
			if (value.isPresent()) return value;
		}
		return Optional.empty();
	}

	private static Optional<Double> length(ObjectEditorView view, String key) {
		return view.parameters().get(key)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}

	private static Vec3 blocks(Vec3d value) {
		return new Vec3(value.x() / MILLIMETERS_PER_BLOCK, value.y() / MILLIMETERS_PER_BLOCK,
			value.z() / MILLIMETERS_PER_BLOCK);
	}

	private static AABB aabb(BoundingBox value) {
		return new AABB(value.min().x() / MILLIMETERS_PER_BLOCK, value.min().y() / MILLIMETERS_PER_BLOCK,
			value.min().z() / MILLIMETERS_PER_BLOCK, value.max().x() / MILLIMETERS_PER_BLOCK,
			value.max().y() / MILLIMETERS_PER_BLOCK, value.max().z() / MILLIMETERS_PER_BLOCK);
	}

	public record Presentation(double widthMm, double heightMm, double depthMm, AABB bounds,
		Vec3 leftWidthHandle, Vec3 rightWidthHandle, Vec3 bottomHeightHandle, Vec3 topHeightHandle, Vec3 heightDimensionLabel, Vec3 dimensionStart, Vec3 dimensionEnd,
		Vec3 dimensionLabel) { }
}
