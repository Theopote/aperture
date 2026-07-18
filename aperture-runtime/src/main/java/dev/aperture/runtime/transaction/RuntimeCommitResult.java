package dev.aperture.runtime.transaction;

import java.util.Objects;
import java.util.Optional;

public record RuntimeCommitResult(Status status, Optional<CommittedRuntimeChange> change, String code, String message) {
	public RuntimeCommitResult {
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(change, "change");
		Objects.requireNonNull(code, "code");
		Objects.requireNonNull(message, "message");
		if (status == Status.COMMITTED && change.isEmpty()) throw new IllegalArgumentException("Committed result requires change");
		if (status == Status.REJECTED && change.isPresent()) throw new IllegalArgumentException("Rejected result cannot contain change");
	}

	public static RuntimeCommitResult committed(CommittedRuntimeChange change) {
		return new RuntimeCommitResult(Status.COMMITTED, Optional.of(change), "", "");
	}

	public static RuntimeCommitResult rejected(String code, String message) {
		return new RuntimeCommitResult(Status.REJECTED, Optional.empty(), code, message);
	}

	public enum Status { COMMITTED, REJECTED }
}
