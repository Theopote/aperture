package dev.aperture.runtime.replication;

import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.*;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.lifecycle.*;
import dev.aperture.runtime.model.command.DefaultCommandBus;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.replication.*;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.world.WorldQueryExecutor;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthoritativeCommandGatewayTest {
 private static final Instant NOW=Instant.parse("2026-07-18T15:00:00Z");
 @Test void validatesAuthorityIsIdempotentAndRepairsTwoReplicasAfterLoss() {
  ArchitecturalRuntime runtime=runtime(); ArchitecturalObjectInstance instance=door(); RuntimeObjectSession created=runtime.create(instance);
  AuthoritativeCommandGateway gateway=new AuthoritativeCommandGateway(runtime,new WorldRef("test:world"));
  ClientReplicaStore clientA=client(); ClientReplicaStore clientB=client();
  JsonAuthorityProtocolCodec codec=new JsonAuthorityProtocolCodec();
  ObjectSnapshotMessage initial=new ObjectSnapshotMessage(1,ReplicaSnapshot.capture(created.instance(),created.state())); clientA.apply(initial); clientB.apply(initial);
  UUID openId=UUID.randomUUID(); CommandRequestMessage open=request(instance.objectId(),openId,"request_open",Map.of(),0,0);
  assertEquals(open,codec.decode(codec.encode(open)));
  var first=gateway.handle(open); assertInstanceOf(CommandAcceptedMessage.class,first.response()); assertEquals(1,runtime.find(instance.objectId()).orElseThrow().objectRevision());
  clientA.apply(first.broadcasts().getFirst());
  var replay=gateway.handle(open); assertEquals(first.response(),replay.response()); assertEquals(1,runtime.find(instance.objectId()).orElseThrow().objectRevision());
  var stale=gateway.handle(request(instance.objectId(),UUID.randomUUID(),"set_lock",Map.of("locked","true"),0,0));
  assertEquals(CommandRejectedMessage.ErrorCode.REVISION_CONFLICT,((CommandRejectedMessage)stale.response()).errorCode());
  var lock=gateway.handle(request(instance.objectId(),UUID.randomUUID(),"set_lock",Map.of("locked","true"),1,1)); assertInstanceOf(CommandAcceptedMessage.class,lock.response());
  assertEquals(ClientReplicaStore.Status.DELTA_APPLIED,clientA.apply(lock.broadcasts().getFirst()).status());
  assertEquals(ClientReplicaStore.Status.RESYNC_REQUIRED,clientB.apply(lock.broadcasts().getFirst()).status());
  ObjectSnapshotMessage repair=gateway.resync(new ObjectResyncRequest(1,instance.objectId(),0,StateRevision.INITIAL,0,"missing delta"));
  assertEquals(ClientReplicaStore.Status.SNAPSHOT_APPLIED,clientB.apply(repair).status());
  assertEquals(clientA.replica(instance.objectId()).orElseThrow(),clientB.replica(instance.objectId()).orElseThrow());
 }
 private static CommandRequestMessage request(ArchitecturalObjectId id,UUID commandId,String type,Map<String,String> payload,long objectRevision,long stateRevision) { return new CommandRequestMessage(1,id,commandId,type,payload,objectRevision,new StateRevision(stateRevision),new ActorRef("test:player"),NOW); }
 private static ClientReplicaStore client() { return new ClientReplicaStore(1,ignored -> DoorStateSchema.SCHEMA); }
 private static ArchitecturalRuntime runtime() { RuntimeObjectConfiguration config=new RuntimeObjectConfiguration(DoorStateSchema.SCHEMA,DoorCapabilities::from,List.of(),KinematicModel.EMPTY); return new DefaultArchitecturalRuntime(new InMemoryRuntimeObjectRepository(),ignored -> config,new DefaultCommandBus(List.of(new RequestOpenDoorHandler(),new SetDoorLockHandler())),WorldQueryExecutor.unavailable()); }
 private static ArchitecturalObjectInstance door() { return new ArchitecturalObjectInstance(1,ArchitecturalObjectId.random(),ArchitecturalTypeId.parse("aperture:door"),new ArchitecturalFamilyId("aperture:opening"),ParameterSet.empty(),Transform3d.identity(),List.of(),Map.of(),0,Map.of()); }
}