package dev.aperture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aperture.math.Transform3d;
import dev.aperture.fabric.placement.McUnits;
import dev.aperture.render.mesh.MeshSection;
import dev.aperture.render.mesh.VertexFormat;
import net.minecraft.core.BlockPos;

/**
 * Emits {@link MeshSection} triangles into a Minecraft {@link VertexConsumer}.
 */
public final class MeshSectionEmitter {
	private MeshSectionEmitter() {
	}

	public static void emit(
		PoseStack.Pose pose,
		VertexConsumer consumer,
		MeshSection section,
		Transform3d transform,
		Transform3d partTransform,
		BlockPos blockPos,
		int color,
		int lightCoords
	) {
		float[] vertices = section.vertices();
		int[] indices = section.indices();

		for (int index : indices) {
			int base = index * VertexFormat.FLOATS_PER_VERTEX;
			dev.aperture.math.Vec3d point = transform.transformPoint(partTransform.transformPoint(new dev.aperture.math.Vec3d(
				vertices[base], vertices[base + 1], vertices[base + 2])));
			float x = toBlockOffset((float) point.x(), blockPos.getX());
			float y = toBlockOffset((float) point.y(), blockPos.getY());
			float z = toBlockOffset((float) point.z(), blockPos.getZ());
			dev.aperture.math.Vec3d normal = transform.transformDirection(partTransform.transformDirection(new dev.aperture.math.Vec3d(
				vertices[base + VertexFormat.NORMAL_OFFSET],
				vertices[base + VertexFormat.NORMAL_OFFSET + 1],
				vertices[base + VertexFormat.NORMAL_OFFSET + 2])));
			float nx = (float) normal.x();
			float ny = (float) normal.y();
			float nz = (float) normal.z();
			float u = vertices[base + VertexFormat.UV_OFFSET];
			float v = vertices[base + VertexFormat.UV_OFFSET + 1];

			consumer.addVertex(pose, x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(0)
				.setLight(lightCoords)
				.setNormal(pose, nx, ny, nz);
		}
	}

	private static float toBlockOffset(float millimeters, int blockPosComponent) {
		return millimeters / (float) McUnits.MILLIMETERS_PER_BLOCK - blockPosComponent;
	}
}
