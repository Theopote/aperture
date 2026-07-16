package dev.aperture.opening.pipeline.golden;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Stable, serializable snapshot of an opening compiler run.
 * Used by golden tests to lock assembly bounds, per-part bounds, and mesh counts.
 */
public record PipelineGoldenSnapshot(
	int schemaVersion,
	String openingTypeId,
	List<String> componentPlanStepIds,
	BoundsSnapshot assemblyBounds,
	BoundsSnapshot cutVolumeBounds,
	List<PartSnapshot> parts
) {
	public static final int CURRENT_SCHEMA_VERSION = 1;

	public PipelineGoldenSnapshot {
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be >= 1");
		}
		componentPlanStepIds = List.copyOf(componentPlanStepIds);
		parts = parts.stream()
			.sorted(Comparator.comparing(PartSnapshot::path))
			.toList();
	}

	public record BoundsSnapshot(double width, double height, double depth) {
	}

	public record PartSnapshot(
		String path,
		String materialSlot,
		String layer,
		BoundsSnapshot bounds,
		int meshTriangleCount
	) {
	}

	public void assertMatches(PipelineGoldenSnapshot expected, double epsilon) {
		Objects.requireNonNull(expected, "expected");
		if (schemaVersion != expected.schemaVersion()) {
			throw new AssertionError("schemaVersion: expected " + expected.schemaVersion() + ", actual " + schemaVersion);
		}
		if (!openingTypeId.equals(expected.openingTypeId())) {
			throw new AssertionError("openingTypeId: expected " + expected.openingTypeId() + ", actual " + openingTypeId);
		}
		if (!componentPlanStepIds.equals(expected.componentPlanStepIds())) {
			throw new AssertionError(
				"componentPlanStepIds: expected " + expected.componentPlanStepIds() + ", actual " + componentPlanStepIds
			);
		}

		assertBounds("assemblyBounds", expected.assemblyBounds(), assemblyBounds, epsilon);
		assertBounds("cutVolumeBounds", expected.cutVolumeBounds(), cutVolumeBounds, epsilon);

		if (parts.size() != expected.parts().size()) {
			throw new AssertionError("parts.size: expected " + expected.parts().size() + ", actual " + parts.size());
		}
		for (int i = 0; i < parts.size(); i++) {
			PartSnapshot actualPart = parts.get(i);
			PartSnapshot expectedPart = expected.parts().get(i);
			String label = "parts[" + actualPart.path() + "]";
			if (!actualPart.path().equals(expectedPart.path())) {
				throw new AssertionError(label + ".path: expected " + expectedPart.path() + ", actual " + actualPart.path());
			}
			if (!actualPart.materialSlot().equals(expectedPart.materialSlot())) {
				throw new AssertionError(
					label + ".materialSlot: expected " + expectedPart.materialSlot() + ", actual " + actualPart.materialSlot()
				);
			}
			if (!actualPart.layer().equals(expectedPart.layer())) {
				throw new AssertionError(label + ".layer: expected " + expectedPart.layer() + ", actual " + actualPart.layer());
			}
			assertBounds(label + ".bounds", expectedPart.bounds(), actualPart.bounds(), epsilon);
			if (actualPart.meshTriangleCount() != expectedPart.meshTriangleCount()) {
				throw new AssertionError(
					label + ".meshTriangleCount: expected "
						+ expectedPart.meshTriangleCount()
						+ ", actual "
						+ actualPart.meshTriangleCount()
				);
			}
		}
	}

	private static void assertBounds(String label, BoundsSnapshot expected, BoundsSnapshot actual, double epsilon) {
		assertClose(label + ".width", expected.width(), actual.width(), epsilon);
		assertClose(label + ".height", expected.height(), actual.height(), epsilon);
		assertClose(label + ".depth", expected.depth(), actual.depth(), epsilon);
	}

	private static void assertClose(String label, double expected, double actual, double epsilon) {
		if (Math.abs(expected - actual) > epsilon) {
			throw new AssertionError(label + ": expected " + expected + ", actual " + actual + " (epsilon " + epsilon + ")");
		}
	}
}
