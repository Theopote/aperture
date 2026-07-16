package dev.aperture.pipeline;

import java.util.Objects;

public record PipelineDiagnostic(
	DiagnosticCode code,
	DiagnosticSeverity severity,
	StageId stage,
	String componentPath,
	String parameterPath,
	String message,
	Throwable cause
) {
	public PipelineDiagnostic {
		Objects.requireNonNull(code, "code");
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(stage, "stage");
		Objects.requireNonNull(message, "message");
	}

	public static PipelineDiagnostic error(DiagnosticCode code, StageId stage, String message, Throwable cause) {
		return new PipelineDiagnostic(code, DiagnosticSeverity.ERROR, stage, null, null, message, cause);
	}

	public PipelineDiagnostic withStage(StageId actualStage) {
		return new PipelineDiagnostic(code, severity, actualStage, componentPath, parameterPath, message, cause);
	}
}