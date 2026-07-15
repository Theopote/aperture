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
		BlockPos blockPos,
		int color,
		int lightCoords
	) {
		float[] vertices = section.vertices();
		int[] indices = section.indices();

		for (int index : indices) {
			int base = index * VertexFormat.FLOATS_PER_VERTEX;
			float x = toBlockOffset(vertices[base] + (float) transform.origin().x(), blockPos.getX());
			float y = toBlockOffset(vertices[base + 1] + (float) transform.origin().y(), blockPos.getY());
			float z = toBlockOffset(vertices[base + 2] + (float) transform.origin().z(), blockPos.getZ());
			float nx = vertices[base + VertexFormat.NORMAL_OFFSET];
			float ny = vertices[base + VertexFormat.NORMAL_OFFSET + 1];
			float nz = vertices[base + VertexFormat.NORMAL_OFFSET + 2];
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
