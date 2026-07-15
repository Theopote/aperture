package dev.aperture.render.data;

import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.mesh.MeshHandle;

import java.util.Objects;
import java.util.Optional;

/**
 * One logical render part backed by a geometry solid snapshot.
 */
public final class RenderPart {
	private final PartId id;
	private final GeometrySolid solid;
	private final long partRevision;
	private MeshHandle meshHandle;

	public RenderPart(PartId id, GeometrySolid solid, long partRevision) {
		this.id = Objects.requireNonNull(id, "id");
		this.solid = Objects.requireNonNull(solid, "solid");
		if (partRevision < 0) {
			throw new IllegalArgumentException("partRevision must be non-negative");
		}
		this.partRevision = partRevision;
	}

	public PartId id() {
		return id;
	}

	public GeometrySolid solid() {
		return solid;
	}

	public long partRevision() {
		return partRevision;
	}

	public Optional<MeshHandle> meshHandle() {
		return Optional.ofNullable(meshHandle);
	}

	public void setMeshHandle(MeshHandle handle) {
		this.meshHandle = Objects.requireNonNull(handle, "handle");
	}

	public RenderPart withSolid(GeometrySolid newSolid, long newRevision) {
		RenderPart updated = new RenderPart(id, newSolid, newRevision);
		updated.meshHandle = meshHandle;
		return updated;
	}
}
