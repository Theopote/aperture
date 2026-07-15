package dev.aperture.geometry.shape;

import dev.aperture.math.BoundingBox;

import java.util.List;

/**
 * Boolean union of multiple solid shapes.
 */
public record UnionShape(List<SolidShape> operands) implements SolidShape {
	public UnionShape {
		if (operands.isEmpty()) {
			throw new IllegalArgumentException("union requires at least one operand");
		}
		operands = List.copyOf(operands);
	}

	@Override
	public BoundingBox bounds() {
		BoundingBox bounds = operands.getFirst().bounds();
		for (int i = 1; i < operands.size(); i++) {
			bounds = bounds.union(operands.get(i).bounds());
		}
		return bounds;
	}
}
