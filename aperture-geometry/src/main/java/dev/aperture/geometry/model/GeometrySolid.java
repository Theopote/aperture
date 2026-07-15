package dev.aperture.geometry.model;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.geometry.shape.SolidShape;

/**
 * A single generated solid belonging to a material slot and render layer.
 */
public record GeometrySolid(
	String componentPath,
	String materialSlot,
	GeometryLayer layer,
	SolidShape shape,
	Transform3d localTransform
) {
	public GeometrySolid {
		if (localTransform == null) {
			localTransform = Transform3d.at(0, 0, 0, Facing.NORTH);
		}
	}

	public BoundingBox bounds() {
		return localTransform.applyTo(shape.bounds());
	}

	public static GeometrySolid box(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		BoundingBox bounds
	) {
		return new GeometrySolid(
			componentPath,
			materialSlot,
			layer,
			new BoxShape(bounds),
			Transform3d.at(0, 0, 0, Facing.NORTH)
		);
	}

	public static GeometrySolid of(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		SolidShape shape
	) {
		return of(componentPath, materialSlot, layer, shape, Transform3d.identity());
	}

	public static GeometrySolid of(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		SolidShape shape,
		Transform3d localTransform
	) {
		return new GeometrySolid(
			componentPath,
			materialSlot,
			layer,
			shape,
			localTransform
		);
	}
}
