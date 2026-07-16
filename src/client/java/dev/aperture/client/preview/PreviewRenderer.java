package dev.aperture.client.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.math.Vec3d;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Renders pipeline preview in the parameter editor.
 * Displays the generated mesh with simple shading.
 */
public class PreviewRenderer {

    private static final float PREVIEW_SCALE = 0.01f; // mm to blocks (1mm = 0.01 blocks)
    private static final int WIREFRAME_COLOR = 0xFFFFFFFF;
    private static final int SOLID_COLOR = 0xFFCCCCCC;

    /**
     * Renders a pipeline result as a preview.
     *
     * @param poseStack Transformation stack
     * @param bufferSource Buffer source for rendering
     * @param result Pipeline result to render
     * @param centerX Center X position in screen space
     * @param centerY Center Y position in screen space
     * @param scale Additional scale factor
     * @param wireframe Whether to render as wireframe
     */
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

        // Center and scale the preview
        poseStack.translate(centerX, centerY, 0);
        poseStack.scale(scale * PREVIEW_SCALE, -scale * PREVIEW_SCALE, scale * PREVIEW_SCALE);

        // Center the geometry at origin
        var bounds = result.geometry().bounds();
        Vec3d center = bounds.min().add(bounds.max()).scale(0.5);
        poseStack.translate(-center.x(), -center.y(), -center.z());

        // Render all mesh parts
        RenderType renderType = wireframe ? RenderType.lines() : RenderType.solid();
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        for (var entry : result.meshes().partsByPath().entrySet()) {
            Mesh mesh = entry.getValue();
            renderMesh(poseStack, consumer, mesh, wireframe);
        }

        poseStack.popPose();
    }

    /**
     * Renders a single mesh.
     */
    private static void renderMesh(
        PoseStack poseStack,
        VertexConsumer consumer,
        Mesh mesh,
        boolean wireframe
    ) {
        Matrix4f matrix = poseStack.last().pose();

        if (wireframe) {
            renderWireframe(consumer, matrix, mesh);
        } else {
            renderSolid(consumer, matrix, mesh);
        }
    }

    /**
     * Renders mesh as wireframe.
     */
    private static void renderWireframe(VertexConsumer consumer, Matrix4f matrix, Mesh mesh) {
        for (int i = 0; i < mesh.faceCount(); i++) {
            int[] indices = mesh.faceIndices(i);

            // Draw triangle edges
            for (int j = 0; j < 3; j++) {
                Vec3d v1 = mesh.vertex(indices[j]);
                Vec3d v2 = mesh.vertex(indices[(j + 1) % 3]);

                consumer.addVertex(matrix, (float) v1.x(), (float) v1.y(), (float) v1.z())
                    .setColor(WIREFRAME_COLOR);

                consumer.addVertex(matrix, (float) v2.x(), (float) v2.y(), (float) v2.z())
                    .setColor(WIREFRAME_COLOR);
            }
        }
    }

    /**
     * Renders mesh as solid.
     */
    private static void renderSolid(VertexConsumer consumer, Matrix4f matrix, Mesh mesh) {
        for (int i = 0; i < mesh.faceCount(); i++) {
            int[] indices = mesh.faceIndices(i);

            // Compute face normal (simple flat shading)
            Vec3d v0 = mesh.vertex(indices[0]);
            Vec3d v1 = mesh.vertex(indices[1]);
            Vec3d v2 = mesh.vertex(indices[2]);

            Vec3d edge1 = v1.subtract(v0);
            Vec3d edge2 = v2.subtract(v0);
            Vec3d normal = edge1.cross(edge2).normalize();

            // Simple directional lighting (light from top-right)
            Vec3d lightDir = new Vec3d(0.5, 0.8, 0.3).normalize();
            double lighting = Math.max(0.3, normal.dot(lightDir));

            int r = (int) ((SOLID_COLOR >> 16 & 0xFF) * lighting);
            int g = (int) ((SOLID_COLOR >> 8 & 0xFF) * lighting);
            int b = (int) ((SOLID_COLOR & 0xFF) * lighting);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;

            // Render triangle
            for (int j = 0; j < 3; j++) {
                Vec3d v = mesh.vertex(indices[j]);
                consumer.addVertex(matrix, (float) v.x(), (float) v.y(), (float) v.z())
                    .setColor(color)
                    .setNormal((float) normal.x(), (float) normal.y(), (float) normal.z());
            }
        }
    }

    /**
     * Computes appropriate scale to fit bounds in viewport.
     */
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
