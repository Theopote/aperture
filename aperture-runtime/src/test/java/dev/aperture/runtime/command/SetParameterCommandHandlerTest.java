package dev.aperture.runtime.command;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.lifecycle.DefaultArchitecturalRuntime;
import dev.aperture.runtime.lifecycle.InMemoryRuntimeObjectRepository;
import dev.aperture.runtime.lifecycle.KinematicModel;
import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.command.SetParameterCommand;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetParameterCommandHandlerTest {
	@Test
	void commitsOneTypedParameterChangeAsOneNewObjectRevision() {
		ArchitecturalObjectId id = ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(1, id,
			ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.of("width", ParameterValue.length(900)), Transform3d.identity(), List.of(), Map.of(), 0, Map.of());
		StateSchema emptyState = StateSchema.builder("test:empty", 1).build();
		RuntimeObjectConfiguration configuration = new RuntimeObjectConfiguration(emptyState,
			ignored -> CapabilitySet.builder().build(), List.of(), KinematicModel.EMPTY);
		DefaultArchitecturalRuntime runtime = new DefaultArchitecturalRuntime(new InMemoryRuntimeObjectRepository(),
			ignored -> configuration, new DefaultCommandBus(List.of(new SetParameterCommandHandler())), WorldQueryExecutor.unavailable());
		runtime.create(instance);

		CommandResult result = runtime.submit(CommandEnvelope.create(
			new SetParameterCommand(new ObjectRef(id), "width", ParameterValue.length(1200)),
			new ActorRef("test:editor"), new WorldRef("test:world"), 0, Instant.EPOCH));

		assertEquals(CommandResult.Status.ACCEPTED, result.status());
		var committed = runtime.find(id).orElseThrow();
		assertEquals(1, committed.objectRevision());
		assertEquals(ParameterValue.length(1200), committed.instance().parameterOverrides().require("width"));
		assertEquals(0, committed.stateRevision().value());
		assertEquals(true, committed.dirtyFlags().persistence());
		assertEquals(true, committed.dirtyFlags().replication());
		assertEquals(true, committed.dirtyFlags().kinematics());
	}
}
