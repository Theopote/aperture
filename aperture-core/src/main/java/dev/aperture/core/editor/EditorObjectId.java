package dev.aperture.core.editor;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for an object in an editor session.
 */
public record EditorObjectId(UUID value) {
	public EditorObjectId {
		Objects.requireNonNull(value, "value");
	}

	public static EditorObjectId random() {
		return new EditorObjectId(UUID.randomUUID());
	}

	public static EditorObjectId of(UUID value) {
		return new EditorObjectId(value);
	}

	public static EditorObjectId fromInstanceId(UUID instanceId) {
		return new EditorObjectId(instanceId);
	}
}
