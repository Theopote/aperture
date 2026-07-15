package dev.aperture.core.editor.history;

import dev.aperture.core.validation.ValidationIssue;

import java.util.List;

/**
 * Outcome of executing or reverting an {@link EditCommand}.
 */
public record EditResult(
	boolean success,
	String description,
	List<ValidationIssue> issues
) {
	public EditResult {
		issues = List.copyOf(issues);
	}

	public static EditResult ok(String description) {
		return new EditResult(true, description, List.of());
	}

	public static EditResult failed(String description, List<ValidationIssue> issues) {
		return new EditResult(false, description, issues);
	}

	public static EditResult failed(String description, String code, String message) {
		return failed(description, List.of(ValidationIssue.error(code, message)));
	}
}
