package dev.aperture.render.pipeline;

import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform-agnostic render backend. Implemented by the Fabric client adapter.
 */
public interface RenderBackend {
	void upload(MeshSection section);

	void draw(DrawCommand command);

	void drawInstanced(InstanceBatch batch);

	void release(MeshAsset asset);
}
