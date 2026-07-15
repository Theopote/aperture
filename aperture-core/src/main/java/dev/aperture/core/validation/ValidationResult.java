package dev.aperture.core.validation;

import java.util.List;

/**
 * Outcome of validating an opening definition, instance, or placement.
 */
public record ValidationResult(List<ValidationIssue> issues) {
	public static final ValidationResult OK = new ValidationResult(List.of());

	public ValidationResult {
		issues = List.copyOf(issues);
	}

	public static ValidationResult of(ValidationIssue... issues) {
		return new ValidationResult(List.of(issues));
	}

	public boolean isValid() {
		return issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
	}

	public ValidationResult merge(ValidationResult other) {
		return new ValidationResult(
			java.util.stream.Stream.concat(issues.stream(), other.issues.stream()).toList()
		);
	}
}
