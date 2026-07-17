package dev.aperture.runtime.model.command;

public interface CommandHandler<T extends ArchitecturalCommand> {
	String commandType();
	Class<T> commandClass();
	CommandResult handle(CommandEnvelope<T> envelope, CommandContext context);
}
