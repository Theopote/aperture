package dev.aperture.runtime.model.behavior;

import java.util.Objects;

public record BehaviorDiagnostic(String code, Severity severity, String message) {
	public enum Severity { INFO, WARNING, ERROR }

	public BehaviorDiagnostic {
		Objects.requireNonNull(code, "code");
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(message, "message");
		if (code.isBlank()) throw new IllegalArgumentException("Diagnostic code must not be blank");
	}

	public static BehaviorDiagnostic error(String code, String message) {
		return new BehaviorDiagnostic(code, Severity.ERROR, message);
	}
}
