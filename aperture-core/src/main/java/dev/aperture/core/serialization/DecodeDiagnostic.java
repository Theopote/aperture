package dev.aperture.core.serialization;

import java.util.Objects;

/** Structured diagnostic emitted by decoding and migration. */
public record DecodeDiagnostic(String code, Severity severity, String message, Throwable cause) {
	public DecodeDiagnostic {
		if (code == null || code.isBlank()) throw new IllegalArgumentException("Diagnostic code must not be blank");
		Objects.requireNonNull(severity, "severity");
		if (message == null || message.isBlank()) throw new IllegalArgumentException("Diagnostic message must not be blank");
	}

	public enum Severity {
		INFO,
		WARNING,
		ERROR
	}
}
