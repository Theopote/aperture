package dev.aperture.render.data;

import dev.aperture.core.placement.PlacementSession;
import dev.aperture.geometry.model.GeometryResult;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ephemeral render document for placement preview or live editing.
 */
public final class PreviewRenderContext {
	private final UUID previewToken;
	private final RenderDocument document;
	private PlacementSession placementSession;

	public PreviewRenderContext(UUID previewToken) {
		this.previewToken = Objects.requireNonNull(previewToken, "previewToken");
		this.document = RenderDocument.forPreview(previewToken);
	}

	public UUID previewToken() {
		return previewToken;
	}

	public RenderDocument document() {
		return document;
	}

	public Optional<PlacementSession> placementSession() {
		return Optional.ofNullable(placementSession);
	}

	public void bind(PlacementSession session) {
		this.placementSession = Objects.requireNonNull(session, "session");
	}

	public RenderDelta updateGeometry(GeometryResult geometry) {
		return document.updateFrom(geometry);
	}
}
