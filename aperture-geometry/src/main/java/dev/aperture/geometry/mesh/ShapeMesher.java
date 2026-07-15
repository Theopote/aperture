package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec2d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.geometry.shape.ExtrusionShape;
import dev.aperture.geometry.shape.SolidShape;
import dev.aperture.geometry.shape.SubtractShape;
import dev.aperture.geometry.shape.UnionShape;

/**
 * Triangulates {@link SolidShape} instances into {@link Mesh} data.
 */
public final class ShapeMesher {
	private ShapeMesher() {
	}

	public static Mesh mesh(SolidShape shape, Transform3d transform) {
		return TransformOps.apply(meshLocal(shape), transform);
	}

	public static Mesh meshLocal(SolidShape shape) {
		return switch (shape) {
			case BoxShape box -> meshBox(box.bounds());
			case ExtrusionShape extrusion -> meshExtrusion(extrusion);
			case UnionShape union -> MeshOps.unionAll(union.operands().stream().map(ShapeMesher::meshLocal).toList());
			case SubtractShape subtract -> {
				Mesh base = meshLocal(subtract.base());
				if (subtract.tool() instanceof BoxShape box) {
					yield MeshCsg.subtractBox(base, box.bounds());
				}
				yield MeshCsg.subtractBox(base, subtract.tool().bounds());
			}
		};
	}

	private static Mesh meshBox(BoundingBox bounds) {
		Vec3d min = bounds.min();
		Vec3d max = bounds.max();
		MeshBuilder builder = new MeshBuilder();

		addBoxFace(builder, min, max, Axis.Z, false);
		addBoxFace(builder, min, max, Axis.Z, true);
		addBoxFace(builder, min, max, Axis.X, false);
		addBoxFace(builder, min, max, Axis.X, true);
		addBoxFace(builder, min, max, Axis.Y, false);
		addBoxFace(builder, min, max, Axis.Y, true);

		return builder.build();
	}

	private static void addBoxFace(MeshBuilder builder, Vec3d min, Vec3d max, Axis axis, boolean positive) {
		switch (axis) {
			case Z -> {
				double z = positive ? max.z() : min.z();
				Vec3d normal = positive ? new Vec3d(0, 0, 1) : new Vec3d(0, 0, -1);
				if (positive) {
					builder.addQuad(
						new Vec3d(min.x(), min.y(), z),
						new Vec3d(max.x(), min.y(), z),
						new Vec3d(max.x(), max.y(), z),
						new Vec3d(min.x(), max.y(), z),
						Vec2d.ZERO, new Vec2d(1, 0), new Vec2d(1, 1), new Vec2d(0, 1)
					);
				} else {
					builder.addQuad(
						new Vec3d(min.x(), min.y(), z),
						new Vec3d(min.x(), max.y(), z),
						new Vec3d(max.x(), max.y(), z),
						new Vec3d(max.x(), min.y(), z),
						Vec2d.ZERO, new Vec2d(0, 1), new Vec2d(1, 1), new Vec2d(1, 0)
					);
				}
			}
			case X -> {
				double x = positive ? max.x() : min.x();
				if (positive) {
					builder.addQuad(
						new Vec3d(x, min.y(), min.z()),
						new Vec3d(x, min.y(), max.z()),
						new Vec3d(x, max.y(), max.z()),
						new Vec3d(x, max.y(), min.z()),
						Vec2d.ZERO, new Vec2d(1, 0), new Vec2d(1, 1), new Vec2d(0, 1)
					);
				} else {
					builder.addQuad(
						new Vec3d(x, min.y(), min.z()),
						new Vec3d(x, max.y(), min.z()),
						new Vec3d(x, max.y(), max.z()),
						new Vec3d(x, min.y(), max.z()),
						Vec2d.ZERO, new Vec2d(0, 1), new Vec2d(1, 1), new Vec2d(1, 0)
					);
				}
			}
			case Y -> {
				double y = positive ? max.y() : min.y();
				if (positive) {
					builder.addQuad(
						new Vec3d(min.x(), y, min.z()),
						new Vec3d(min.x(), y, max.z()),
						new Vec3d(max.x(), y, max.z()),
						new Vec3d(max.x(), y, min.z()),
						Vec2d.ZERO, new Vec2d(0, 1), new Vec2d(1, 1), new Vec2d(1, 0)
					);
				} else {
					builder.addQuad(
						new Vec3d(min.x(), y, min.z()),
						new Vec3d(max.x(), y, min.z()),
						new Vec3d(max.x(), y, max.z()),
						new Vec3d(min.x(), y, max.z()),
						Vec2d.ZERO, new Vec2d(1, 0), new Vec2d(1, 1), new Vec2d(0, 1)
					);
				}
			}
		}
	}

	private static Mesh meshExtrusion(ExtrusionShape extrusion) {
		MeshBuilder builder = new MeshBuilder();
		Vec3d pathVector = extrusion.pathVector();

		for (int i = 0; i < extrusion.profile().segmentCount(); i++) {
			Vec2d profileA = extrusion.profile().point(i);
			Vec2d profileB = extrusion.profile().point(i + 1);
			Vec3d startA = extrusion.mapProfilePoint(profileA);
			Vec3d startB = extrusion.mapProfilePoint(profileB);
			Vec3d endA = startA.add(pathVector);
			Vec3d endB = startB.add(pathVector);

			builder.addQuad(
				startA, endA, endB, startB,
				profileA, profileA, profileB, profileB
			);
		}

		addCap(builder, extrusion, extrusion.pathStart(), false);
		addCap(builder, extrusion, extrusion.pathEnd(), true);
		return builder.build();
	}

	private static void addCap(MeshBuilder builder, ExtrusionShape extrusion, Vec3d origin, boolean endCap) {
		Vec3d pathVector = endCap ? extrusion.pathVector() : Vec3d.ZERO;

		for (int i = 1; i < extrusion.profile().segmentCount() - 1; i++) {
			Vec3d center = extrusion.mapProfilePoint(extrusion.profile().point(0)).add(pathVector);
			Vec3d a = extrusion.mapProfilePoint(extrusion.profile().point(i)).add(pathVector);
			Vec3d b = extrusion.mapProfilePoint(extrusion.profile().point(i + 1)).add(pathVector);
			if (endCap) {
				builder.addTriangle(center, a, b, Vec2d.ZERO, new Vec2d(i, 0), new Vec2d(i + 1, 0));
			} else {
				builder.addTriangle(center, b, a, Vec2d.ZERO, new Vec2d(i + 1, 0), new Vec2d(i, 0));
			}
		}
	}

	private enum Axis {
		X, Y, Z
	}
}
