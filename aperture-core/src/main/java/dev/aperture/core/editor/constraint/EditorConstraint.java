package dev.aperture.core.editor.constraint;

import java.util.Objects;

/**
 * Declarative spatial rule enforced while manipulating an opening in the editor.
 */
public record EditorConstraint(
	EditorConstraintKind kind,
	String message
) {
	public EditorConstraint {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}
}
