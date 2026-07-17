package dev.aperture.runtime.model.command;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StatePersistence;
import dev.aperture.runtime.model.state.StatePropertyDefinition;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandBusTest {
	@Test
	void routesTypedCommandAndReturnsStateIntentWithoutMutation() {
		Fixture fixture = fixture(3);
		DefaultCommandBus bus = new DefaultCommandBus(List.of(openHandler(new AtomicInteger())));
		CommandResult result = bus.dispatch(envelope(new RequestOpenCommand(fixture.target), 3), fixture.context);

		assertEquals(CommandResult.Status.ACCEPTED, result.status());
		assertEquals(1, result.statePatches().size());
		assertEquals(StateRevision.INITIAL, fixture.context.state().revision());
	}

	@Test
	void rejectsRevisionConflictBeforeInvokingHandler() {
		Fixture fixture = fixture(4);
		AtomicInteger calls = new AtomicInteger();
		DefaultCommandBus bus = new DefaultCommandBus(List.of(openHandler(calls)));
		CommandResult result = bus.dispatch(envelope(new RequestOpenCommand(fixture.target), 3), fixture.context);

		assertEquals(CommandResult.Status.REJECTED, result.status());
		assertEquals("command.revision_conflict", result.diagnostics().getFirst().code());
		assertEquals(0, calls.get());
	}

	@Test
	void transactionPreflightsEveryCommandBeforeEvaluation() {
		Fixture fixture = fixture(2);
		AtomicInteger calls = new AtomicInteger();
		DefaultCommandBus bus = new DefaultCommandBus(List.of(openHandler(calls)));
		CommandTransaction transaction = CommandTransaction.of(List.of(
			envelope(new RequestOpenCommand(fixture.target), 2),
			envelope(new RequestCloseCommand(fixture.target), 2)
		));

		TransactionResult result = bus.dispatch(transaction, fixture.context);
		assertEquals(TransactionResult.Status.REJECTED, result.status());
		assertEquals("command.handler_missing", result.commandResults().getFirst().diagnostics().getFirst().code());
		assertEquals(0, calls.get());
	}

	private static CommandHandler<RequestOpenCommand> openHandler(AtomicInteger calls) {
		return new CommandHandler<>() {
			@Override public String commandType() { return "aperture:request_open"; }
			@Override public Class<RequestOpenCommand> commandClass() { return RequestOpenCommand.class; }
			@Override public CommandResult handle(CommandEnvelope<RequestOpenCommand> envelope, CommandContext context) {
				calls.incrementAndGet();
				return CommandResult.accepted(List.of(new StatePatch(
					context.state().revision(), Map.of("targetOpenRatio", StateValue.number(1)), envelope.timestamp())));
			}
		};
	}

	private static <T extends ArchitecturalCommand> CommandEnvelope<T> envelope(T command, long revision) {
		return CommandEnvelope.create(command, new ActorRef("test:actor"), new WorldRef("test:world"), revision, Instant.EPOCH);
	}

	private static Fixture fixture(long revision) {
		ArchitecturalObjectId id = ArchitecturalObjectId.random();
		ObjectRef target = new ObjectRef(id);
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, id, ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(), revision, Map.of());
		StateSchema schema = StateSchema.builder("aperture:door", 1)
			.property("targetOpenRatio", StatePropertyDefinition.number(
				0, 0.0, 1.0, StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
			.build();
		return new Fixture(target, new CommandContext(
			instance, RuntimeState.initial(schema), CapabilitySet.empty(), WorldQuery.unavailable()));
	}

	private record Fixture(ObjectRef target, CommandContext context) { }
}
