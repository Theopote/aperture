package dev.aperture.runtime.lifecycle;

import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.command.CommandContext;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldEffect;
import dev.aperture.runtime.model.world.WorldEffectResult;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeWorldEffectOrderingTest {
	@Test
	void executesWorldEffectsOnlyAfterAValidStateCommit() {
		AtomicInteger executions = new AtomicInteger();
		ArchitecturalObjectInstance instance = doorInstance();
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(
			DoorStateSchema.SCHEMA, DoorCapabilities::from, List.of(), KinematicModel.EMPTY);
		DefaultArchitecturalRuntime runtime = new DefaultArchitecturalRuntime(
			new InMemoryRuntimeObjectRepository(), ignored -> configuration,
			new DefaultCommandBus(List.of(handler())), WorldQueryExecutor.unavailable(), effect -> {
				executions.incrementAndGet();
				return WorldEffectResult.applied();
			});
		runtime.create(instance);

		CommandResult result = runtime.submit(CommandEnvelope.create(
			new RequestOpenCommand(new ObjectRef(instance.objectId())), new ActorRef("test:actor"),
			new WorldRef("test:world"), 0, Instant.EPOCH));

		assertEquals(CommandResult.Status.REJECTED, result.status());
		assertEquals(0, executions.get());
		assertEquals(0, runtime.find(instance.objectId()).orElseThrow().objectRevision());
	}

	private static CommandHandler<RequestOpenCommand> handler() {
		return new CommandHandler<>() {
			@Override public String commandType() { return "aperture:request_open"; }
			@Override public Class<RequestOpenCommand> commandClass() { return RequestOpenCommand.class; }

			@Override
			public CommandResult handle(CommandEnvelope<RequestOpenCommand> envelope, CommandContext context) {
				WorldEffect effect = () -> "test:must_not_execute";
				StatePatch invalid = new StatePatch(context.state().revision(),
					Map.of(DoorStateSchema.OPEN_RATIO, StateValue.number(2)), envelope.timestamp());
				return new CommandResult(CommandResult.Status.ACCEPTED, List.of(invalid), List.of(),
					List.of(effect), List.of());
			}
		};
	}

	private static ArchitecturalObjectInstance doorInstance() {
		return new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
	}
}
