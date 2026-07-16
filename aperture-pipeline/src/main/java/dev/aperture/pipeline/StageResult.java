package dev.aperture.pipeline;

import java.util.Objects;
import java.util.Optional;

public sealed interface StageResult<T> {
	record Success<T>(T value) implements StageResult<T> {
		public Success { Objects.requireNonNull(value, "value cannot be null"); }
		@Override public boolean isSuccess() { return true; }
		@Override public T getValue() { return value; }
		@Override public Optional<PipelineDiagnostic> diagnostic() { return Optional.empty(); }
	}

	record Failure<T>(PipelineDiagnostic error) implements StageResult<T> {
		public Failure { Objects.requireNonNull(error, "error cannot be null"); }
		public Failure(DiagnosticCode code, String message, Throwable cause) {
			this(PipelineDiagnostic.error(code, StageId.CUSTOM, message, cause));
		}
		public Failure(DiagnosticCode code, String message) { this(code, message, null); }
		public Failure(String message, Throwable cause) { this(DiagnosticCode.INTERNAL_ERROR, message, cause); }
		public Failure(String message) { this(message, null); }
		@Override public boolean isSuccess() { return false; }
		@Override public T getValue() { throw new IllegalStateException(error.message(), error.cause()); }
		@Override public Optional<PipelineDiagnostic> diagnostic() { return Optional.of(error); }
	}

	record Skipped<T>(String reason, T cachedValue) implements StageResult<T> {
		public Skipped { Objects.requireNonNull(reason, "reason"); Objects.requireNonNull(cachedValue, "cachedValue"); }
		@Override public boolean isSuccess() { return true; }
		@Override public T getValue() { return cachedValue; }
		@Override public Optional<PipelineDiagnostic> diagnostic() { return Optional.empty(); }
	}

	boolean isSuccess();
	T getValue();
	Optional<PipelineDiagnostic> diagnostic();
	default Optional<String> getErrorMessage() { return diagnostic().map(PipelineDiagnostic::message); }
	default Optional<Throwable> getCause() { return diagnostic().map(PipelineDiagnostic::cause).filter(Objects::nonNull); }
}