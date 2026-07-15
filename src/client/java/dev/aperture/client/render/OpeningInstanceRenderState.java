package dev.aperture.client.render;

import dev.aperture.math.Transform3d;
import dev.aperture.geometry.material.MaterialBindingSet;
import dev.aperture.render.mesh.MeshAsset;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Extracted render state for a committed opening instance.
 */
public final class OpeningInstanceRenderState extends BlockEntityRenderState {
	private MeshAsset meshAsset = MeshAsset.empty(dev.aperture.render.mesh.LODLevel.FULL);
	private MaterialBindingSet materialBindings = new MaterialBindingSet(java.util.Map.of());
	private Transform3d transform = Transform3d.at(0, 0, 0, dev.aperture.math.Facing.NORTH);
	private boolean hasMesh;

	public MeshAsset meshAsset() {
		return meshAsset;
	}

	public MaterialBindingSet materialBindings() {
		return materialBindings;
	}

	public Transform3d transform() {
		return transform;
	}

	public boolean hasMesh() {
		return hasMesh;
	}

	public void setFrom(InstanceRenderCache.CachedInstanceRender cached) {
		this.meshAsset = cached.meshAsset();
		this.materialBindings = cached.materialBindings();
		this.transform = cached.transform();
		this.hasMesh = !cached.meshAsset().partIds().isEmpty();
	}

	public void clear() {
		this.meshAsset = MeshAsset.empty(dev.aperture.render.mesh.LODLevel.FULL);
		this.materialBindings = new MaterialBindingSet(java.util.Map.of());
		this.hasMesh = false;
	}
}
