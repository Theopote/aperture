package dev.aperture.runtime.model.command;

import java.util.Objects;

public record CommandDiagnostic(String code, Severity severity, String message) {
	public enum Severity { INFO, WARNING, ERROR }
	public CommandDiagnostic {
		Objects.requireNonNull(code, "code");
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(message, "message");
		if (code.isBlank()) throw new IllegalArgumentException("Diagnostic code must not be blank");
	}
	public static CommandDiagnostic error(String code, String message) {
		return new CommandDiagnostic(code, Severity.ERROR, message);
	}
}
