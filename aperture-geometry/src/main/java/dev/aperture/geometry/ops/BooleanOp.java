package dev.aperture.geometry.ops;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.geometry.shape.SolidShape;
import dev.aperture.geometry.shape.SubtractShape;
import dev.aperture.geometry.shape.UnionShape;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level constructive solid geometry operations on {@link SolidShape} trees.
 */
public final class BooleanOp {
	private BooleanOp() {
	}

	public static SolidShape subtract(SolidShape base, SolidShape tool) {
		return new SubtractShape(base, tool);
	}

	public static SolidShape subtractBox(SolidShape base, BoundingBox toolBounds) {
		return subtract(base, new BoxShape(toolBounds));
	}

	public static SolidShape union(SolidShape first, SolidShape second) {
		return new UnionShape(List.of(first, second));
	}

	public static SolidShape unionAll(List<SolidShape> operands) {
		return new UnionShape(List.copyOf(operands));
	}

	public static SolidShape subtractAll(SolidShape base, List<BoundingBox> tools) {
		SolidShape result = base;
		for (BoundingBox tool : tools) {
			result = subtractBox(result, tool);
		}
		return result;
	}

	public static SolidShape subtractBoxes(SolidShape base, BoundingBox... tools) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (BoundingBox tool : tools) {
			boxes.add(tool);
		}
		return subtractAll(base, boxes);
	}
}
