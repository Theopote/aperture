package dev.aperture.runtime.replication;

import dev.aperture.runtime.lifecycle.ArchitecturalRuntime;
import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.CommandEnvelope;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.command.RequestCloseCommand;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.command.SetLockCommand;
import dev.aperture.runtime.model.command.SetParameterCommand;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.capability.StandardCapabilities;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.replication.*;
import dev.aperture.runtime.model.state.StateRevision;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-only ingress: validates untrusted requests, commits through the runtime, and emits canonical replication. */
public final class AuthoritativeCommandGateway {
 public static final int PROTOCOL_VERSION = 1;
 private static final int MAX_IDEMPOTENCY_ENTRIES = 1024;
 private final ArchitecturalRuntime runtime;
 private final WorldRef world;
 private final Map<UUID, Outcome> completed = new LinkedHashMap<>();

 public AuthoritativeCommandGateway(ArchitecturalRuntime runtime, WorldRef world) {
  this.runtime=Objects.requireNonNull(runtime); this.world=Objects.requireNonNull(world);
 }

 public synchronized Outcome handle(CommandRequestMessage request) {
  Outcome replay=completed.get(request.commandId());
  if(replay!=null) return replay;
  RuntimeObjectSession before=runtime.find(request.objectId()).orElse(null);
  if(request.protocolVersion()!=PROTOCOL_VERSION) return remember(request.commandId(), reject(request, CommandRejectedMessage.ErrorCode.PROTOCOL_MISMATCH, "Unsupported protocol version", before));
  if(before==null) return remember(request.commandId(), reject(request, CommandRejectedMessage.ErrorCode.OBJECT_NOT_FOUND, "Object is not active", null));
  if(before.objectRevision()!=request.expectedObjectRevision() || !before.stateRevision().equals(request.expectedStateRevision())) {
   return remember(request.commandId(), reject(request, CommandRejectedMessage.ErrorCode.REVISION_CONFLICT, "Authoritative revision differs from client expectation", before));
  }
  ArchitecturalCommand command;
  try { command=decode(request, before); }
  catch(IllegalArgumentException ex) { return remember(request.commandId(), reject(request, CommandRejectedMessage.ErrorCode.COMMAND_UNSUPPORTED, ex.getMessage(), before)); }
  CommandEnvelope<ArchitecturalCommand> envelope=new CommandEnvelope<>(request.commandId(), command, request.actor(), world, request.expectedObjectRevision(), request.timestamp(), request.commandId(), null, Map.of("transport","network"));
  CommandResult result=runtime.submit(envelope);
  RuntimeObjectSession after=runtime.find(request.objectId()).orElse(before);
  if(result.status()==CommandResult.Status.REJECTED) {
   String message=result.diagnostics().isEmpty()?"Command rejected":result.diagnostics().getFirst().message();
   return remember(request.commandId(), reject(request, CommandRejectedMessage.ErrorCode.COMMAND_REJECTED, message, after));
  }
  CommandAcceptedMessage accepted=new CommandAcceptedMessage(PROTOCOL_VERSION, request.objectId(), request.commandId(), after.objectRevision(), after.stateRevision(), after.state().timestamp());
  ReplicationMessage update=before.stateRevision().equals(after.stateRevision())
   ? new ObjectSnapshotMessage(PROTOCOL_VERSION,ReplicaSnapshot.capture(after.instance(),after.state()))
   : StateDeltaFactory.between(before.instance(),before.state(),after.instance(),after.state());
  return remember(request.commandId(), new Outcome(accepted, List.of(update), false));
 }

 public ObjectSnapshotMessage resync(ObjectResyncRequest request) {
  if(request.protocolVersion()!=PROTOCOL_VERSION) throw new IllegalArgumentException("Unsupported protocol version");
  RuntimeObjectSession session=runtime.find(request.objectId()).orElseThrow(() -> new IllegalArgumentException("Object is not active"));
  return new ObjectSnapshotMessage(PROTOCOL_VERSION, ReplicaSnapshot.capture(session.instance(), session.state()));
 }

 private ArchitecturalCommand decode(CommandRequestMessage request, RuntimeObjectSession session) {
  ObjectRef target=new ObjectRef(request.objectId());
  return switch(request.commandType()) {
   case "request_open" -> new RequestOpenCommand(target);
   case "request_close" -> new RequestCloseCommand(target);
   case "toggle_open" -> session.capabilities().requireCapability(StandardCapabilities.OPENABLE).targetRatio() > 0
    ? new RequestCloseCommand(target) : new RequestOpenCommand(target);
   case "set_lock" -> new SetLockCommand(target, parseBoolean(request.payload(), "locked"));
   case "set_parameter" -> new SetParameterCommand(target, required(request.payload(), "parameter"), parseParameter(request.payload()));
   default -> throw new IllegalArgumentException("Unsupported command type: "+request.commandType());
  };
 }
 private static ParameterValue parseParameter(Map<String,String> payload) {
  String type=required(payload,"valueType"); String value=required(payload,"value");
  try {
   return switch(type) {
    case "LENGTH" -> ParameterValue.length(Double.parseDouble(value));
    case "ANGLE" -> ParameterValue.angle(Double.parseDouble(value));
    case "COUNT" -> ParameterValue.count(Integer.parseInt(value));
    case "NUMBER" -> ParameterValue.number(Double.parseDouble(value));
    case "ENUM" -> ParameterValue.enumValue(value);
    case "BOOL" -> ParameterValue.bool(parseBoolean(payload,"value"));
    case "MATERIAL_REF" -> ParameterValue.materialRef(value);
    default -> throw new IllegalArgumentException("Unsupported parameter value type: "+type);
   };
  } catch(NumberFormatException error) { throw new IllegalArgumentException("Invalid "+type+" parameter value",error); }
 }
 private static String required(Map<String,String> payload,String name) {
  String value=payload.get(name); if(value==null||value.isBlank()) throw new IllegalArgumentException("Missing payload: "+name); return value;
 } private static boolean parseBoolean(Map<String,String> payload,String name) {
  String value=payload.get(name); if(!"true".equals(value)&&!"false".equals(value)) throw new IllegalArgumentException("Invalid boolean payload: "+name); return Boolean.parseBoolean(value);
 }
 private Outcome reject(CommandRequestMessage request, CommandRejectedMessage.ErrorCode code, String message, RuntimeObjectSession session) {
  long objectRevision=session==null?-1:session.objectRevision(); StateRevision stateRevision=session==null?StateRevision.INITIAL:session.stateRevision();
  return new Outcome(new CommandRejectedMessage(PROTOCOL_VERSION, request.objectId(), request.commandId(), code, message, objectRevision, stateRevision, request.timestamp()), List.of(), false);
 }
 private Outcome remember(UUID id, Outcome outcome) {
  completed.put(id,outcome); while(completed.size()>MAX_IDEMPOTENCY_ENTRIES) completed.remove(completed.keySet().iterator().next()); return outcome;
 }
 public record Outcome(ReplicationMessage response, List<ReplicationMessage> broadcasts, boolean replayed) {
  public Outcome { Objects.requireNonNull(response); broadcasts=List.copyOf(broadcasts); }
 }
}