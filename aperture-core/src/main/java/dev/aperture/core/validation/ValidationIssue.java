package dev.aperture.core.validation;

/**
 * A single validation finding.
 */
public record ValidationIssue(ValidationSeverity severity, String code, String message) {
	public static ValidationIssue error(String code, String message) {
		return new ValidationIssue(ValidationSeverity.ERROR, code, message);
	}

	public static ValidationIssue warning(String code, String message) {
		return new ValidationIssue(ValidationSeverity.WARNING, code, message);
	}
}
