package dev.aperture.geometry.mesh;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec2d;
import dev.aperture.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Precise constructive solid geometry for triangle meshes.
 */
public final class MeshCsg {
	private static final double EPSILON = 1.0e-6;

	private MeshCsg() {
	}

	public static Mesh subtractBox(Mesh mesh, BoundingBox box) {
		MeshBuilder builder = new MeshBuilder();
		for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
			int index = triangle * 3;
			Vec3d a = readPosition(mesh, mesh.indices()[index]);
			Vec3d b = readPosition(mesh, mesh.indices()[index + 1]);
			Vec3d c = readPosition(mesh, mesh.indices()[index + 2]);
			emitOutsideFragments(a, b, c, box, builder, mesh, triangle);
		}
		if (builder.isEmpty()) {
			throw new IllegalStateException("subtraction removed all mesh geometry");
		}
		return builder.build();
	}

	private static void emitOutsideFragments(
		Vec3d a,
		Vec3d b,
		Vec3d c,
		BoundingBox box,
		MeshBuilder builder,
		Mesh source,
		int triangleIndex
	) {
		if (isFullyOutside(a, b, c, box)) {
			Vec2d uvA = sampleUv(source, triangleIndex, 0);
			Vec2d uvB = sampleUv(source, triangleIndex, 1);
			Vec2d uvC = sampleUv(source, triangleIndex, 2);
			builder.addTriangle(a, b, c, uvA, uvB, uvC);
			return;
		}
		if (isFullyInside(a, b, c, box)) {
			return;
		}

		List<List<Vec3d>> polygons = subtractAabb(List.of(a, b, c), box);
		for (List<Vec3d> polygon : polygons) {
			triangulateFan(polygon, builder, source, triangleIndex);
		}
	}

	private static boolean isFullyOutside(Vec3d a, Vec3d b, Vec3d c, BoundingBox box) {
		double minX = Math.min(a.x(), Math.min(b.x(), c.x()));
		double maxX = Math.max(a.x(), Math.max(b.x(), c.x()));
		if (maxX < box.min().x() || minX > box.max().x()) {
			return true;
		}

		double minY = Math.min(a.y(), Math.min(b.y(), c.y()));
		double maxY = Math.max(a.y(), Math.max(b.y(), c.y()));
		if (maxY < box.min().y() || minY > box.max().y()) {
			return true;
		}

		double minZ = Math.min(a.z(), Math.min(b.z(), c.z()));
		double maxZ = Math.max(a.z(), Math.max(b.z(), c.z()));
		return maxZ < box.min().z() || minZ > box.max().z();
	}

	private static boolean isFullyInside(Vec3d a, Vec3d b, Vec3d c, BoundingBox box) {
		return isInsideBox(a, box) && isInsideBox(b, box) && isInsideBox(c, box);
	}

	private static List<List<Vec3d>> subtractAabb(List<Vec3d> polygon, BoundingBox box) {
		List<List<Vec3d>> polygons = List.of(polygon);
		polygons = splitOutside(polygons, box.min().x(), box.max().x(), Axis.X);
		polygons = splitOutside(polygons, box.min().y(), box.max().y(), Axis.Y);
		polygons = splitOutside(polygons, box.min().z(), box.max().z(), Axis.Z);
		return polygons.stream()
			.filter(poly -> shouldKeepPolygon(poly, box))
			.toList();
	}

	private static boolean shouldKeepPolygon(List<Vec3d> polygon, BoundingBox box) {
		if (polygon.size() < 3) {
			return false;
		}
		return polygon.stream().anyMatch(point -> !isInsideBox(point, box));
	}

	private static boolean isInsideBox(Vec3d point, BoundingBox box) {
		return point.x() >= box.min().x() - EPSILON && point.x() <= box.max().x() + EPSILON
			&& point.y() >= box.min().y() - EPSILON && point.y() <= box.max().y() + EPSILON
			&& point.z() >= box.min().z() - EPSILON && point.z() <= box.max().z() + EPSILON;
	}

	private static List<List<Vec3d>> splitOutside(List<List<Vec3d>> polygons, double min, double max, Axis axis) {
		List<List<Vec3d>> result = new ArrayList<>();
		for (List<Vec3d> polygon : polygons) {
			List<Vec3d> low = clipPolygon(polygon, axis, min, KeepSide.LOW);
			if (low.size() >= 3) {
				result.add(low);
			}
			List<Vec3d> high = clipPolygon(polygon, axis, max, KeepSide.HIGH);
			if (high.size() >= 3) {
				result.add(high);
			}
		}
		return result;
	}

	private static List<Vec3d> clipPolygon(List<Vec3d> input, Axis axis, double planeValue, KeepSide keepSide) {
		if (input.isEmpty()) {
			return List.of();
		}
		List<Vec3d> output = new ArrayList<>();
		Vec3d previous = input.getLast();
		boolean previousKept = isKept(previous, axis, planeValue, keepSide);

		for (Vec3d current : input) {
			boolean currentKept = isKept(current, axis, planeValue, keepSide);
			if (currentKept) {
				if (!previousKept) {
					output.add(intersect(previous, current, axis, planeValue));
				}
				output.add(current);
			} else if (previousKept) {
				output.add(intersect(previous, current, axis, planeValue));
			}
			previous = current;
			previousKept = currentKept;
		}
		return output;
	}

	private static boolean isKept(Vec3d point, Axis axis, double planeValue, KeepSide keepSide) {
		double coordinate = coordinate(point, axis);
		return keepSide == KeepSide.LOW
			? coordinate <= planeValue + EPSILON
			: coordinate >= planeValue - EPSILON;
	}

	private static Vec3d intersect(Vec3d a, Vec3d b, Axis axis, double planeValue) {
		double start = coordinate(a, axis);
		double end = coordinate(b, axis);
		double t = (planeValue - start) / (end - start);
		return new Vec3d(
			lerp(a.x(), b.x(), t),
			lerp(a.y(), b.y(), t),
			lerp(a.z(), b.z(), t)
		);
	}

	private static void triangulateFan(List<Vec3d> polygon, MeshBuilder builder, Mesh source, int triangleIndex) {
		if (polygon.size() < 3) {
			return;
		}
		Vec2d uvA = sampleUv(source, triangleIndex, 0);
		Vec3d anchor = polygon.getFirst();
		for (int i = 1; i < polygon.size() - 1; i++) {
			builder.addTriangle(anchor, polygon.get(i), polygon.get(i + 1), uvA, uvA, uvA);
		}
	}

	private static Vec2d sampleUv(Mesh mesh, int triangleIndex, int corner) {
		int vertexIndex = mesh.indices()[triangleIndex * 3 + corner];
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX + 6;
		float[] vertices = mesh.vertices();
		return new Vec2d(vertices[offset], vertices[offset + 1]);
	}

	private static Vec3d readPosition(Mesh mesh, int vertexIndex) {
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
		float[] vertices = mesh.vertices();
		return new Vec3d(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
	}

	private static double coordinate(Vec3d point, Axis axis) {
		return switch (axis) {
			case X -> point.x();
			case Y -> point.y();
			case Z -> point.z();
		};
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	private enum Axis {
		X, Y, Z
	}

	private enum KeepSide {
		LOW,
		HIGH
	}
}
