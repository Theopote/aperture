package dev.aperture.geometry.recipe;

import java.util.List;

/**
 * Declarative geometry assembly: a sequence of kernel ops compiled from opening steps
 * or NodeCraft graphs, then evaluated by {@link GeometryRecipeExecutor}.
 */
public record GeometryRecipe(List<GeometryOp> ops) {
	public GeometryRecipe {
		ops = List.copyOf(ops);
	}
}
