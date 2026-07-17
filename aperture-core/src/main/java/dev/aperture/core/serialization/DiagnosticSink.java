package dev.aperture.core.serialization;

/** Receives structured decode diagnostics without imposing a logging backend. */
@FunctionalInterface
public interface DiagnosticSink {
	void report(DecodeDiagnostic diagnostic);

	static DiagnosticSink noop() {
		return ignored -> { };
	}
}
