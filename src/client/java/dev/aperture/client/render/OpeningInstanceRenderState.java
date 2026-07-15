package dev.aperture.client.render;

import dev.aperture.core.geometry.Transform3d;
import dev.aperture.render.mesh.MeshAsset;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Extracted render state for a committed opening instance.
 */
public final class OpeningInstanceRenderState extends BlockEntityRenderState {
	private MeshAsset meshAsset = MeshAsset.empty(dev.aperture.render.mesh.LODLevel.FULL);
	private Transform3d transform = Transform3d.at(0, 0, 0, dev.aperture.core.geometry.Facing.NORTH);
	private boolean hasMesh;

	public MeshAsset meshAsset() {
		return meshAsset;
	}

	public Transform3d transform() {
		return transform;
	}

	public boolean hasMesh() {
		return hasMesh;
	}

	public void setFrom(InstanceRenderCache.CachedInstanceRender cached) {
		this.meshAsset = cached.meshAsset();
		this.transform = cached.transform();
		this.hasMesh = !cached.meshAsset().partIds().isEmpty();
	}

	public void clear() {
		this.meshAsset = MeshAsset.empty(dev.aperture.render.mesh.LODLevel.FULL);
		this.hasMesh = false;
	}
}
