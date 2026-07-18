package dev.aperture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aperture.client.render.material.FabricMaterialGraphics;
import dev.aperture.math.Transform3d;
import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.runtime.kinematic.KinematicPose;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.material.MaterialBinding;
import dev.aperture.geometry.material.MaterialBindingSet;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshBufferCache;
import dev.aperture.render.mesh.MeshSection;
import dev.aperture.render.pipeline.DrawCommand;
import dev.aperture.render.pipeline.InstanceBatch;
import dev.aperture.render.pipeline.RenderBackend;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Fabric implementation of the platform-agnostic {@link RenderBackend}.
 */
public final class FabricRenderBackend implements RenderBackend {
	public static final FabricRenderBackend INSTANCE = new FabricRenderBackend();
	private static final Identifier FALLBACK_TEXTURE = Identifier.withDefaultNamespace("block/stone");

	private final MeshBufferCache cache = new MeshBufferCache();
	private @Nullable RenderFrameContext frameContext;

	private FabricRenderBackend() {
	}

	public void beginFrame(
		PoseStack poseStack,
		SubmitNodeCollector queue,
		int lightCoords,
		Transform3d transform,
		BlockPos blockPos,
		MaterialBindingSet materialBindings
	) {
		this.frameContext = new RenderFrameContext(poseStack, queue, lightCoords, transform, blockPos, materialBindings, KinematicPose.IDENTITY);
	}

	public void endFrame() {
		this.frameContext = null;
	}

	public void submitAsset(
		PoseStack poseStack,
		SubmitNodeCollector queue,
		int lightCoords,
		MeshAsset asset,
		MaterialBindingSet materialBindings,
		Transform3d transform,
		BlockPos blockPos
	) {
		submitAsset(poseStack, queue, lightCoords, asset, materialBindings, transform, blockPos, KinematicPose.IDENTITY);
	}

	public void submitAsset(
		PoseStack poseStack,
		SubmitNodeCollector queue,
		int lightCoords,
		MeshAsset asset,
		MaterialBindingSet materialBindings,
		Transform3d transform,
		BlockPos blockPos,
		KinematicPose kinematicPose
	) {
		beginFrame(poseStack, queue, lightCoords, transform, blockPos, materialBindings);
		this.frameContext = new RenderFrameContext(poseStack, queue, lightCoords, transform, blockPos, materialBindings, kinematicPose);
		for (MeshSection section : asset.sections().values()) {
			if (materialBindings.get(section.partId()).isEmpty()) {
				continue;
			}
			upload(section);
			drawSection(section);
		}
		endFrame();
	}

	public void submitAssetGhost(
		PoseStack poseStack,
		SubmitNodeCollector queue,
		int lightCoords,
		MeshAsset asset,
		MaterialBindingSet materialBindings,
		Transform3d transform,
		BlockPos blockPos,
		boolean valid
	) {
		beginFrame(poseStack, queue, lightCoords, transform, blockPos, materialBindings);
		for (MeshSection section : asset.sections().values()) {
			if (materialBindings.get(section.partId()).isEmpty()) {
				continue;
			}
			upload(section);
			drawSection(section, valid);
		}
		endFrame();
	}

	@Override
	public void upload(MeshSection section) {
		cache.track(section);
	}

	@Override
	public void draw(DrawCommand command) {
		drawSection(command.meshSection());
	}

	@Override
	public void drawInstanced(InstanceBatch batch) {
		if (frameContext == null) {
			return;
		}

		drawSection(batch.meshSection());
	}

	@Override
	public void release(MeshAsset asset) {
		for (MeshSection section : asset.sections().values()) {
			cache.release(section.handle());
		}
	}

	private void drawSection(MeshSection section) {
		drawSection(section, null);
	}

	private void drawSection(MeshSection section, @Nullable Boolean ghostValid) {
		RenderFrameContext ctx = this.frameContext;
		if (ctx == null) {
			return;
		}

		MaterialBinding binding = ctx.materialBindings().get(section.partId()).orElse(null);
		FabricMaterialGraphics.ResolvedMaterialDraw draw;
		if (binding != null) {
			draw = ghostValid != null
				? FabricMaterialGraphics.resolveGhost(binding, ghostValid)
				: FabricMaterialGraphics.resolveCommitted(binding.material());
		} else {
			draw = fallbackDraw(section.layer());
		}

		Transform3d transform = ctx.transform();
		BlockPos blockPos = ctx.blockPos();
		int lightCoords = ctx.lightCoords();
		int tintArgb = draw.tintArgb();

		ctx.queue().submitCustomGeometry(
			ctx.poseStack(),
			draw.renderType(),
			(pose, buffer) -> MeshSectionEmitter.emit(
				pose,
				buffer,
				section,
				transform,
				ctx.kinematicPose().transformFor(new ComponentPath(section.partId().componentPath())),
				blockPos,
				tintArgb,
				lightCoords
			)
		);
	}

	private static FabricMaterialGraphics.ResolvedMaterialDraw fallbackDraw(GeometryLayer layer) {
		RenderType renderType = switch (layer) {
			case OPAQUE -> RenderTypes.entitySolid(FALLBACK_TEXTURE);
			case CUTOUT -> RenderTypes.entityCutout(FALLBACK_TEXTURE);
			case TRANSLUCENT -> RenderTypes.entityTranslucent(FALLBACK_TEXTURE);
		};
		int tint = switch (layer) {
			case OPAQUE -> 0xFFC8A878;
			case CUTOUT -> 0xFF909090;
			case TRANSLUCENT -> 0xAA88CCFF;
		};
		return new FabricMaterialGraphics.ResolvedMaterialDraw(renderType, tint);
	}

	private record RenderFrameContext(
		PoseStack poseStack,
		SubmitNodeCollector queue,
		int lightCoords,
		Transform3d transform,
		BlockPos blockPos,
			MaterialBindingSet materialBindings,
		KinematicPose kinematicPose
	) {
	}
}
