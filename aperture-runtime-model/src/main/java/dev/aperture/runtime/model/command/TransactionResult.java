package dev.aperture.runtime.model.command;

import java.util.List;

public record TransactionResult(Status status, List<CommandResult> commandResults) {
	public enum Status { COMMITTED, REJECTED }
	public TransactionResult { commandResults = List.copyOf(commandResults); }
}
