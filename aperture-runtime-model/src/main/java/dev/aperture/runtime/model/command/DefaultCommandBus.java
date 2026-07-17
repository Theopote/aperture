package dev.aperture.runtime.model.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Type-safe routing and intent-level atomic transaction evaluation. */
public final class DefaultCommandBus implements CommandBus {
	private final Map<String, CommandHandler<?>> handlers;

	public DefaultCommandBus(List<? extends CommandHandler<?>> handlers) {
		Map<String, CommandHandler<?>> indexed = new LinkedHashMap<>();
		for (CommandHandler<?> handler : handlers) {
			Objects.requireNonNull(handler, "handler");
			if (indexed.putIfAbsent(handler.commandType(), handler) != null) {
				throw new IllegalArgumentException("Duplicate command handler: " + handler.commandType());
			}
		}
		this.handlers = Map.copyOf(indexed);
	}

	@Override
	public CommandResult dispatch(CommandEnvelope<? extends ArchitecturalCommand> envelope, CommandContext context) {
		CommandResult preflight = preflight(envelope, context);
		if (preflight != null) return preflight;
		return invoke(requireHandler(envelope.command().commandType()), envelope, context);
	}

	@Override
	public TransactionResult dispatch(CommandTransaction transaction, CommandContext context) {
		List<CommandResult> preflight = transaction.commands().stream()
			.map(command -> preflight(command, context))
			.filter(Objects::nonNull)
			.toList();
		if (!preflight.isEmpty()) return new TransactionResult(TransactionResult.Status.REJECTED, preflight);

		List<CommandResult> evaluated = transaction.commands().stream()
			.map(command -> invoke(requireHandler(command.command().commandType()), command, context))
			.toList();
		if (evaluated.stream().anyMatch(result -> result.status() == CommandResult.Status.REJECTED)) {
			return new TransactionResult(TransactionResult.Status.REJECTED, evaluated);
		}
		return new TransactionResult(TransactionResult.Status.COMMITTED, evaluated);
	}

	private CommandResult preflight(CommandEnvelope<? extends ArchitecturalCommand> envelope, CommandContext context) {
		if (!envelope.command().target().objectId().equals(context.instance().objectId())) {
			return CommandResult.rejected("command.target_mismatch", "Command target does not match context object");
		}
		if (envelope.expectedObjectRevision() != context.instance().revision()) {
			return CommandResult.rejected("command.revision_conflict",
				"Expected object revision " + envelope.expectedObjectRevision() + " but was " + context.instance().revision());
		}
		CommandHandler<?> handler = handlers.get(envelope.command().commandType());
		if (handler == null) return CommandResult.rejected("command.handler_missing", "No handler for " + envelope.command().commandType());
		if (!handler.commandClass().isInstance(envelope.command())) {
			return CommandResult.rejected("command.type_mismatch", "Command payload does not match registered handler");
		}
		return null;
	}

	private CommandHandler<?> requireHandler(String commandType) { return handlers.get(commandType); }

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static CommandResult invoke(
		CommandHandler handler, CommandEnvelope<? extends ArchitecturalCommand> envelope, CommandContext context
	) {
		try {
			CommandResult result = handler.handle(envelope, context);
			return Objects.requireNonNull(result, "Command handler returned null");
		} catch (RuntimeException failure) {
			return CommandResult.rejected("command.handler_failed", failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
		}
	}
}
