package dev.aperture.runtime.model.command;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Ordered atomic unit of command intent evaluation. */
public record CommandTransaction(UUID transactionId, List<CommandEnvelope<? extends ArchitecturalCommand>> commands) {
	public CommandTransaction {
		Objects.requireNonNull(transactionId, "transactionId");
		commands = List.copyOf(commands);
		if (commands.isEmpty()) throw new IllegalArgumentException("Transaction must contain at least one command");
	}

	public static CommandTransaction of(List<CommandEnvelope<? extends ArchitecturalCommand>> commands) {
		return new CommandTransaction(UUID.randomUUID(), commands);
	}
}
