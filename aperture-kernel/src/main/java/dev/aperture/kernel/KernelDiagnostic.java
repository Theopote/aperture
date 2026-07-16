package dev.aperture.kernel;
import dev.aperture.pipeline.DiagnosticSeverity;
import dev.aperture.pipeline.StageId;
import java.util.Objects;
public record KernelDiagnostic(KernelErrorCode code, DiagnosticSeverity severity, StageId stage,
	String componentPath, String parameterPath, String message, Throwable cause) {
	public KernelDiagnostic { Objects.requireNonNull(code); Objects.requireNonNull(severity); Objects.requireNonNull(stage); Objects.requireNonNull(message); }
}