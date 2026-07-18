package dev.aperture.editor.model.read;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.time.Instant;
import java.util.Optional;

public record EditorDiagnostic(Severity severity, String code, String message,
	Optional<ArchitecturalObjectId> objectId, Optional<String> path, String stage,
	Instant timestamp, String suggestedAction, boolean resolved) {
	public enum Severity { INFO, WARNING, ERROR }
}
