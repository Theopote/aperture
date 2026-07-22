package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ArchitecturalPickingServiceTest {
	private static final WorldRay RAY = new WorldRay(Vec3d.ZERO, new Vec3d(1, 0, 0));

	@Test
	void priorityWinsBeforeDistance() {
		PickCandidate nearObject = candidate(1, PickPriority.ARCHITECTURAL_OBJECT);
		PickCandidate farManipulator = candidate(10, PickPriority.ACTIVE_MANIPULATOR);
		var service = new ArchitecturalPickingService(List.of(source("object", nearObject), source("gizmo", farManipulator)));

		PickResult result = service.pick(RAY, PickContext.empty()).orElseThrow();

		assertEquals(PickPriority.ACTIVE_MANIPULATOR, result.priority());
		assertEquals("gizmo", result.sourceId());
	}

	@Test
	void nearestCandidateWinsWithinOnePriority() {
		var service = new ArchitecturalPickingService(List.of(
			source("far", candidate(5, PickPriority.ARCHITECTURAL_OBJECT)),
			source("near", candidate(2, PickPriority.ARCHITECTURAL_OBJECT))));

		assertEquals("near", service.pick(RAY, PickContext.empty()).orElseThrow().sourceId());
	}

	@Test
	void noCandidatesProducesNoSelection() {
		var service = new ArchitecturalPickingService(List.of(source("empty")));
		assertTrue(service.pick(RAY, PickContext.empty()).isEmpty());
	}

	private static PickSource source(String id, PickCandidate... candidates) {
		return new PickSource() {
			@Override public String id() { return id; }
			@Override public List<PickCandidate> pick(WorldRay ray, PickContext context) { return List.of(candidates); }
		};
	}

	private static PickCandidate candidate(double distance, PickPriority priority) {
		return new PickCandidate(ArchitecturalObjectId.random(), Optional.empty(), Optional.empty(),
			PickCandidate.HitKind.OBJECT, new Vec3d(distance, 0, 0), new Vec3d(0, 1, 0), distance, priority);
	}
}
