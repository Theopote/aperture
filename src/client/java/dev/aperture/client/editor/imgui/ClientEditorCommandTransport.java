package dev.aperture.client.editor.imgui;

import dev.aperture.client.runtime.ClientRuntimeReplicas;
import dev.aperture.editor.model.command.EditorCommandSubmission;
import dev.aperture.editor.model.command.EditorCommandTransport;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.command.SetParameterArchitecturalCommand;
import dev.aperture.editor.model.read.DiagnosticsModel;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.fabric.network.ApertureReplicationPayload;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.RequestCloseCommand;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.command.SetLockCommand;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.replication.CommandAcceptedMessage;
import dev.aperture.runtime.model.replication.CommandRejectedMessage;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.replication.AuthoritativeCommandGateway;
import dev.aperture.runtime.replication.JsonReplicationMessageCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Async Fabric transport; server responses are correlated without mutating client replicas. */
final class ClientEditorCommandTransport implements EditorCommandTransport {
	private final JsonReplicationMessageCodec codec = new JsonReplicationMessageCodec();
	private final DiagnosticsModel diagnostics;
	private final Map<UUID, ArchitecturalObjectId> pending = new ConcurrentHashMap<>();

	ClientEditorCommandTransport(DiagnosticsModel diagnostics) {
		this.diagnostics = diagnostics;
		ClientRuntimeReplicas.addMessageListener(this::onMessage);
	}

	@Override
	public EditorCommandSubmission submit(UUID commandId, ArchitecturalCommand command, ExpectedRevision revision) {
		if (!ClientPlayNetworking.canSend(ApertureReplicationPayload.TYPE))
			return rejected(commandId, "Server does not accept Aperture editor commands", revision);
		Map<String, String> payload;
		String type;
		if (command instanceof SetParameterArchitecturalCommand set) {
			type = "set_parameter";
			payload = parameterPayload(set.parameterKey(), set.value());
		} else if (command instanceof RequestOpenCommand) { type = "request_open"; payload = Map.of(); }
		else if (command instanceof RequestCloseCommand) { type = "request_close"; payload = Map.of(); }
		else if (command instanceof SetLockCommand lock) { type = "set_lock"; payload = Map.of("locked", Boolean.toString(lock.locked())); }
		else return rejected(commandId, "Unsupported editor command: " + command.commandType(), revision);
		Minecraft client = Minecraft.getInstance();
		String actor = "minecraft:" + client.getUser().getProfileId();
		CommandRequestMessage request = new CommandRequestMessage(AuthoritativeCommandGateway.PROTOCOL_VERSION,
			command.target().objectId(), commandId, type, payload, revision.objectRevision(),
			new StateRevision(revision.stateRevision()), new ActorRef(actor), Instant.now());
		pending.put(commandId, command.target().objectId());
		ClientPlayNetworking.send(new ApertureReplicationPayload(codec.encode(request)));
		return new EditorCommandSubmission(commandId, EditorCommandSubmission.Status.PENDING,
			"Submitted to authoritative server", revision.objectRevision(), revision.stateRevision());
	}

	private void onMessage(ReplicationMessage message) {
		if (message instanceof CommandAcceptedMessage accepted) pending.remove(accepted.commandId());
		else if (message instanceof CommandRejectedMessage rejected) {
			ArchitecturalObjectId id = pending.remove(rejected.commandId());
			if (id == null) return;
			EditorDiagnostic.Severity severity = EditorDiagnostic.Severity.ERROR;
			diagnostics.add(new EditorDiagnostic(severity, rejected.errorCode().name().toLowerCase(), rejected.message(),
				Optional.of(id), Optional.empty(), "server", Instant.now(),
				rejected.errorCode() == CommandRejectedMessage.ErrorCode.REVISION_CONFLICT ? "Resync object" : "Review command", false));
		}
	}

	private static Map<String, String> parameterPayload(String key, ParameterValue value) {
		String raw = switch (value) {
			case ParameterValue.LengthValue v -> Double.toString(v.millimeters());
			case ParameterValue.AngleValue v -> Double.toString(v.degrees());
			case ParameterValue.CountValue v -> Integer.toString(v.value());
			case ParameterValue.NumberValue v -> Double.toString(v.value());
			case ParameterValue.EnumValue v -> v.value();
			case ParameterValue.BoolValue v -> Boolean.toString(v.value());
			case ParameterValue.MaterialRefValue v -> v.raw();
		};
		return Map.of("parameter", key, "valueType", value.type().name(), "value", raw);
	}

	private static EditorCommandSubmission rejected(UUID id, String message, ExpectedRevision revision) {
		return new EditorCommandSubmission(id, EditorCommandSubmission.Status.REJECTED, message,
			revision.objectRevision(), revision.stateRevision());
	}
}
