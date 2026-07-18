package dev.aperture.runtime.kinematic;

import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Shared transformed bounds used by coarse dynamic collision and runtime picking. */
public final class DynamicSpatialIndex {
	private final List<DynamicBounds> bounds;

	private DynamicSpatialIndex(List<DynamicBounds> bounds) {
		this.bounds = List.copyOf(bounds);
	}

	public static DynamicSpatialIndex evaluate(
		Map<ComponentPath, BoundingBox> localBounds, KinematicPose pose
	) {
		Objects.requireNonNull(localBounds, "localBounds");
		Objects.requireNonNull(pose, "pose");
		List<DynamicBounds> evaluated = new ArrayList<>();
		localBounds.forEach((component, bounds) -> {
			Transform3d transform = pose.transformFor(component);
			evaluated.add(new DynamicBounds(component, bounds, transform.applyTo(bounds)));
		});
		return new DynamicSpatialIndex(evaluated);
	}

	/** Coarse transformed AABBs suitable for the first dynamic collision adapter. */
	public List<DynamicBounds> collisionBounds() { return bounds; }

	/** Returns the nearest transformed component AABB hit by a normalized or non-normalized ray. */
	public Optional<PickHit> pick(Vec3d origin, Vec3d direction, double maximumDistance) {
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(direction, "direction");
		if (direction.lengthSquared() == 0) throw new IllegalArgumentException("Ray direction must not be zero");
		if (!Double.isFinite(maximumDistance) || maximumDistance < 0) {
			throw new IllegalArgumentException("maximumDistance must be finite and non-negative");
		}
		Vec3d normalized = direction.normalize();
		return bounds.stream()
			.map(bound -> intersect(origin, normalized, bound))
			.flatMap(Optional::stream)
			.filter(hit -> hit.distance() <= maximumDistance)
			.min(Comparator.comparingDouble(PickHit::distance));
	}

	private static Optional<PickHit> intersect(Vec3d origin, Vec3d direction, DynamicBounds candidate) {
		double near = 0;
		double far = Double.POSITIVE_INFINITY;
		double[] origins = {origin.x(), origin.y(), origin.z()};
		double[] directions = {direction.x(), direction.y(), direction.z()};
		double[] minima = {candidate.worldBounds().min().x(), candidate.worldBounds().min().y(), candidate.worldBounds().min().z()};
		double[] maxima = {candidate.worldBounds().max().x(), candidate.worldBounds().max().y(), candidate.worldBounds().max().z()};
		for (int axis = 0; axis < 3; axis++) {
			if (Math.abs(directions[axis]) < 1.0e-12) {
				if (origins[axis] < minima[axis] || origins[axis] > maxima[axis]) return Optional.empty();
				continue;
			}
			double first = (minima[axis] - origins[axis]) / directions[axis];
			double second = (maxima[axis] - origins[axis]) / directions[axis];
			if (first > second) { double swap = first; first = second; second = swap; }
			near = Math.max(near, first);
			far = Math.min(far, second);
			if (near > far) return Optional.empty();
		}
		return Optional.of(new PickHit(candidate.component(), near, origin.add(direction.scale(near))));
	}

	public record DynamicBounds(ComponentPath component, BoundingBox localBounds, BoundingBox worldBounds) {
		public DynamicBounds {
			Objects.requireNonNull(component, "component");
			Objects.requireNonNull(localBounds, "localBounds");
			Objects.requireNonNull(worldBounds, "worldBounds");
		}
	}

	public record PickHit(ComponentPath component, double distance, Vec3d position) { }
}
