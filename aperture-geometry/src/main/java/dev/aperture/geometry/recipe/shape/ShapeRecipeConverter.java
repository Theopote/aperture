package dev.aperture.geometry.recipe.shape;

import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.geometry.shape.ExtrusionShape;
import dev.aperture.geometry.shape.SolidShape;
import dev.aperture.geometry.shape.SubtractShape;
import dev.aperture.geometry.shape.UnionShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts evaluated {@link SolidShape} trees back into declarative {@link ShapeRecipe} where possible.
 */
public final class ShapeRecipeConverter {
	private ShapeRecipeConverter() {
	}

	public static ShapeRecipe fromSolid(SolidShape shape) {
		return switch (shape) {
			case BoxShape(var bounds) -> new BoxRecipe(bounds);
			case ExtrusionShape extrusion -> new ExtrudeLinearRecipe(
				extrusion.profile(),
				extrusion.pathStart(),
				extrusion.pathEnd(),
				extrusion.profileU(),
				extrusion.profileV()
			);
			case SubtractShape(var base, var tool) -> convertSubtract(base, tool);
			case UnionShape(var operands) -> new UnionRecipe(
				operands.stream().map(ShapeRecipeConverter::fromSolid).toList()
			);
		};
	}

	private static ShapeRecipe convertSubtract(SolidShape base, SolidShape tool) {
		List<dev.aperture.math.BoundingBox> boxes = collectBoxTools(tool);
		if (boxes.isEmpty()) {
			return new SolidShapeRecipe(new SubtractShape(base, tool));
		}
		return new SubtractBoxesRecipe(fromSolid(base), boxes);
	}

	private static List<dev.aperture.math.BoundingBox> collectBoxTools(SolidShape tool) {
		if (tool instanceof BoxShape(var bounds)) {
			return List.of(bounds);
		}
		if (tool instanceof UnionShape(var operands)) {
			List<dev.aperture.math.BoundingBox> boxes = new ArrayList<>();
			for (SolidShape operand : operands) {
				if (operand instanceof BoxShape(var bounds)) {
					boxes.add(bounds);
				} else {
					return List.of();
				}
			}
			return boxes;
		}
		return List.of();
	}
}
