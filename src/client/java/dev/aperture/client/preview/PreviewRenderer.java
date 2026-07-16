package dev.aperture.client.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.math.Vec3d;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;

/**
 * Renders pipeline preview in the parameter editor.
 * Displays the generated mesh with simple shading.
 */
public class PreviewRenderer {

    private static final float PREVIEW_SCALE = 0.01f; // mm to blocks (1mm = 0.01 blocks)
    private static final int WIREFRAME_COLOR = 0xFFFFFFFF;
    private static final int SOLID_COLOR = 0xFFCCCCCC;

    public static void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        PipelineResult result,
        float centerX,
        float centerY,
        float scale,
        boolean wireframe
    ) {
        if (result == null || result.meshes().partsByPath().isEmpty()) {
            return;
        }

        poseStack.pushPose();

        poseStack.translate(centerX, centerY, 0);
        poseStack.scale(scale * PREVIEW_SCALE, -scale * PREVIEW_SCALE, scale * PREVIEW_SCALE);

        var bounds = result.geometry().bounds();
        Vec3d center = bounds.min().add(bounds.max()).scale(0.5);
        poseStack.translate(-center.x(), -center.y(), -center.z());

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());

        for (var entry : result.meshes().partsByPath().entrySet()) {
            Mesh mesh = entry.getValue();
            renderMeshWireframe(poseStack, consumer, mesh);
        }

        poseStack.popPose();
    }

    private static void renderMeshWireframe(PoseStack poseStack, VertexConsumer consumer, Mesh mesh) {
        Matrix4f matrix = poseStack.last().pose();
        int[] indices = mesh.indices();
        float[] vertices = mesh.vertices();
        int triangleCount = mesh.triangleCount();
        int stride = Mesh.FLOATS_PER_VERTEX;

        for (int t = 0; t < triangleCount; t++) {
            int i0 = indices[t * 3] * stride;
            int i1 = indices[t * 3 + 1] * stride;
            int i2 = indices[t * 3 + 2] * stride;

            float x0 = vertices[i0], y0 = vertices[i0 + 1], z0 = vertices[i0 + 2];
            float x1 = vertices[i1], y1 = vertices[i1 + 1], z1 = vertices[i1 + 2];
            float x2 = vertices[i2], y2 = vertices[i2 + 1], z2 = vertices[i2 + 2];

            emitLine(consumer, matrix, x0, y0, z0, x1, y1, z1);
            emitLine(consumer, matrix, x1, y1, z1, x2, y2, z2);
            emitLine(consumer, matrix, x2, y2, z2, x0, y0, z0);
        }
    }

    private static void emitLine(VertexConsumer consumer, Matrix4f matrix,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2) {
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        consumer.addVertex(matrix, x1, y1, z1).setColor(WIREFRAME_COLOR).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(WIREFRAME_COLOR).setNormal(nx, ny, nz);
    }

    public static float computeFitScale(PipelineResult result, float viewportWidth, float viewportHeight) {
        if (result == null) {
            return 1.0f;
        }

        var bounds = result.geometry().bounds();
        double maxDim = Math.max(
            Math.max(bounds.width(), bounds.height()),
            bounds.depth()
        );

        if (maxDim < 0.001) {
            return 1.0f;
        }

        float availableSpace = Math.min(viewportWidth, viewportHeight) * 0.8f;
        return (float) (availableSpace / (maxDim * PREVIEW_SCALE));
    }
}
