package dev.aperture.runtime.replication;

import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.DoorCapabilities;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.command.SetParameterCommandHandler;
import dev.aperture.runtime.lifecycle.*;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.replication.CommandAcceptedMessage;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthoritySetParameterTest {
	@Test void decodesAndCommitsTypedParameterPayload() {
		ArchitecturalObjectId id=ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance=new ArchitecturalObjectInstance(1,id,ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"),ParameterSet.of("width",ParameterValue.length(900)),
			Transform3d.identity(),List.of(),Map.of(),0,Map.of());
		RuntimeObjectConfiguration config=new RuntimeObjectConfiguration(DoorStateSchema.SCHEMA,DoorCapabilities::from,List.of(),KinematicModel.EMPTY);
		ArchitecturalRuntime runtime=new DefaultArchitecturalRuntime(new InMemoryRuntimeObjectRepository(),ignored -> config,
			new DefaultCommandBus(List.of(new SetParameterCommandHandler())),WorldQueryExecutor.unavailable());
		runtime.create(instance);
		CommandRequestMessage request=new CommandRequestMessage(1,id,UUID.randomUUID(),"set_parameter",
			Map.of("parameter","width","valueType","LENGTH","value","1200.0"),0,StateRevision.INITIAL,
			new ActorRef("test:editor"),Instant.EPOCH);

		var outcome=new AuthoritativeCommandGateway(runtime,new WorldRef("test:world")).handle(request);

		assertInstanceOf(CommandAcceptedMessage.class,outcome.response());
		assertEquals(ParameterValue.length(1200),runtime.find(id).orElseThrow().instance().parameterOverrides().require("width"));
		assertEquals(1,runtime.find(id).orElseThrow().objectRevision());
	}
}
