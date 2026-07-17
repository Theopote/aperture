package dev.aperture.runtime.model.command;

public interface CommandBus {
	CommandResult dispatch(CommandEnvelope<? extends ArchitecturalCommand> envelope, CommandContext context);
	TransactionResult dispatch(CommandTransaction transaction, CommandContext context);
}
