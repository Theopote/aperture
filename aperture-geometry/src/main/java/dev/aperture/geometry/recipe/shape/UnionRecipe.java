package dev.aperture.geometry.recipe.shape;

import java.util.List;

/**
 * Boolean union of multiple shape recipes — used to merge profile rails and other operands.
 */
public record UnionRecipe(List<ShapeRecipe> operands) implements ShapeRecipe {
	public UnionRecipe {
		if (operands.isEmpty()) {
			throw new IllegalArgumentException("union requires at least one operand");
		}
		operands = List.copyOf(operands);
	}
}
