package dev.aperture.render.data;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Authoritative render-side view of one opening or preview.
 * Applies geometry deltas and tracks which parts need mesh recompilation.
 */
public final class RenderDocument {
	private final UUID documentId;
	private final PartRegistry parts = new PartRegistry();
	private final DirtyPartTracker dirtyParts = new DirtyPartTracker();
	private GeometryResult geometry;
	private RenderRevision revision = RenderRevision.ZERO;
	private long partRevisionCounter;

	public RenderDocument(UUID documentId) {
		this.documentId = Objects.requireNonNull(documentId, "documentId");
	}

	public static RenderDocument forPreview(UUID previewToken) {
		return new RenderDocument(previewToken);
	}

	public static RenderDocument forInstance(UUID instanceId) {
		return new RenderDocument(instanceId);
	}

	public UUID documentId() {
		return documentId;
	}

	public RenderRevision revision() {
		return revision;
	}

	public Optional<GeometryResult> geometry() {
		return Optional.ofNullable(geometry);
	}

	public PartRegistry parts() {
		return parts;
	}

	public DirtyPartTracker dirtyParts() {
		return dirtyParts;
	}

	/**
	 * Applies a new geometry snapshot and returns the structural delta.
	 */
	public RenderDelta updateFrom(GeometryResult nextGeometry) {
		Objects.requireNonNull(nextGeometry, "nextGeometry");
		RenderDelta delta = RenderDeltaEngine.compute(geometry, nextGeometry);
		applyDelta(delta, nextGeometry);
		geometry = nextGeometry;
		revision = revision.next();
		dirtyParts.markAll(delta.dirty());
		return delta;
	}

	private void applyDelta(RenderDelta delta, GeometryResult nextGeometry) {
		for (PartId removed : delta.removed()) {
			parts.remove(removed);
		}

		for (GeometrySolid solid : nextGeometry.solids()) {
			PartId id = PartId.of(solid.componentPath());
			if (delta.added().contains(id) || delta.changed().contains(id)) {
				parts.put(new RenderPart(id, solid, ++partRevisionCounter));
			}
		}
	}
}
