package dev.aperture.runtime.replication;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.replication.CommandAcceptedMessage;
import dev.aperture.runtime.model.replication.CommandCommittedReplicationEvent;
import dev.aperture.runtime.model.replication.CommandRejectedMessage;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.model.replication.EventDeltaMessage;
import dev.aperture.runtime.model.replication.ObjectRemovedMessage;
import dev.aperture.runtime.model.replication.ObjectResyncRequest;
import dev.aperture.runtime.model.replication.ObjectSnapshotMessage;
import dev.aperture.runtime.model.replication.ReplicaSnapshot;
import dev.aperture.runtime.model.replication.ReplicatedEvent;
import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.model.replication.ReplicationMessageCodec;
import dev.aperture.runtime.model.replication.StateDeltaMessage;
import dev.aperture.runtime.model.replication.StateTransitionReplicationEvent;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.state.StateValueType;
import dev.aperture.runtime.persistence.ArchitecturalObjectSnapshotJsonCodec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Canonical JSON codec for every allow-listed runtime replication message. */
public final class JsonReplicationMessageCodec implements ReplicationMessageCodec {
	private static final Gson GSON = new Gson();
	private final ArchitecturalObjectSnapshotJsonCodec snapshotCodec = new ArchitecturalObjectSnapshotJsonCodec();

	@Override
	public String encode(ReplicationMessage message) {
		JsonObject root = new JsonObject();
		root.addProperty("protocolVersion", message.protocolVersion());
		root.addProperty("objectId", message.objectId().toString());
		switch (message) {
			case ObjectSnapshotMessage snapshot -> writeSnapshot(root, snapshot);
			case StateDeltaMessage delta -> writeStateDelta(root, delta);
			case EventDeltaMessage delta -> writeEventDelta(root, delta);
			case ObjectRemovedMessage removed -> writeRemoved(root, removed);
			case CommandRequestMessage request -> writeCommandRequest(root, request);
			case CommandAcceptedMessage accepted -> writeCommandAccepted(root, accepted);
			case CommandRejectedMessage rejected -> writeCommandRejected(root, rejected);
			case ObjectResyncRequest request -> writeResyncRequest(root, request);
			default -> throw new IllegalArgumentException("Message is not a client replication payload: " + message.getClass().getName());
		}
		return GSON.toJson(root);
	}

	@Override
	public ReplicationMessage decode(String encoded) {
		JsonObject root = JsonParser.parseString(encoded).getAsJsonObject();
		int protocolVersion = root.get("protocolVersion").getAsInt();
		ArchitecturalObjectId objectId = ArchitecturalObjectId.parse(root.get("objectId").getAsString());
		return switch (requiredString(root, "kind")) {
			case "snapshot" -> readSnapshot(root, protocolVersion, objectId);
			case "state_delta" -> readStateDelta(root, protocolVersion, objectId);
			case "event_delta" -> readEventDelta(root, protocolVersion, objectId);
			case "removed" -> readRemoved(root, protocolVersion, objectId);
			case "command_request" -> readCommandRequest(root, protocolVersion, objectId);
			case "command_accepted" -> readCommandAccepted(root, protocolVersion, objectId);
			case "command_rejected" -> readCommandRejected(root, protocolVersion, objectId);
			case "resync_request" -> readResyncRequest(root, protocolVersion, objectId);
			default -> throw new IllegalArgumentException("Unknown replication message kind");
		};
	}

	private void writeSnapshot(JsonObject root, ObjectSnapshotMessage message) {
		root.addProperty("kind", "snapshot");
		ReplicaSnapshot snapshot = message.snapshot();
		ArchitecturalObjectSnapshot wire = new ArchitecturalObjectSnapshot(1, snapshot.instance(),
			snapshot.distributedState(), snapshot.stateRevision(), snapshot.timestamp(), List.of());
		root.add("snapshot", JsonParser.parseString(snapshotCodec.encode(wire)));
	}

	private ObjectSnapshotMessage readSnapshot(JsonObject root, int protocolVersion, ArchitecturalObjectId objectId) {
		ArchitecturalObjectSnapshot wire = snapshotCodec.decode(GSON.toJson(root.get("snapshot")));
		if (!wire.instance().objectId().equals(objectId)) {
			throw new IllegalArgumentException("Snapshot object ID does not match envelope");
		}
		ArchitecturalObjectInstance encoded = wire.instance();
		ArchitecturalObjectInstance projection = new ArchitecturalObjectInstance(
			encoded.schemaVersion(), encoded.objectId(), encoded.typeId(), encoded.familyId(),
			encoded.parameterOverrides(), encoded.transform(), encoded.hostBindings(), Map.of(),
			encoded.revision(), encoded.metadata());
		return new ObjectSnapshotMessage(protocolVersion, new ReplicaSnapshot(projection,
			wire.persistentState(), wire.stateRevision(), wire.stateTimestamp()));
	}

	private static void writeStateDelta(JsonObject root, StateDeltaMessage message) {
		root.addProperty("kind", "state_delta");
		root.addProperty("baseObjectRevision", message.baseObjectRevision());
		root.addProperty("resultingObjectRevision", message.resultingObjectRevision());
		root.addProperty("baseStateRevision", message.baseStateRevision().value());
		root.addProperty("resultingStateRevision", message.resultingStateRevision().value());
		root.add("updates", writeState(message.updates()));
		root.addProperty("timestamp", message.timestamp().toString());
	}

	private static StateDeltaMessage readStateDelta(
		JsonObject root, int protocolVersion, ArchitecturalObjectId objectId
	) {
		return new StateDeltaMessage(protocolVersion, objectId,
			root.get("baseObjectRevision").getAsLong(), root.get("resultingObjectRevision").getAsLong(),
			new StateRevision(root.get("baseStateRevision").getAsLong()),
			new StateRevision(root.get("resultingStateRevision").getAsLong()),
			readState(root.getAsJsonObject("updates")), Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeEventDelta(JsonObject root, EventDeltaMessage message) {
		root.addProperty("kind", "event_delta");
		root.addProperty("baseSequence", message.baseSequence());
		root.addProperty("resultingSequence", message.resultingSequence());
		root.addProperty("timestamp", message.timestamp().toString());
		JsonArray events = new JsonArray();
		for (ReplicatedEvent event : message.events()) events.add(writeEvent(event));
		root.add("events", events);
	}

	private static EventDeltaMessage readEventDelta(
		JsonObject root, int protocolVersion, ArchitecturalObjectId objectId
	) {
		List<ReplicatedEvent> events = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("events")) events.add(readEvent(element.getAsJsonObject()));
		return new EventDeltaMessage(protocolVersion, objectId, root.get("baseSequence").getAsLong(),
			root.get("resultingSequence").getAsLong(), events, Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeRemoved(JsonObject root, ObjectRemovedMessage message) {
		root.addProperty("kind", "removed");
		root.addProperty("finalObjectRevision", message.finalObjectRevision());
		root.addProperty("timestamp", message.timestamp().toString());
	}

	private static ObjectRemovedMessage readRemoved(
		JsonObject root, int protocolVersion, ArchitecturalObjectId objectId
	) {
		return new ObjectRemovedMessage(protocolVersion, objectId, root.get("finalObjectRevision").getAsLong(),
			Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeCommandRequest(JsonObject root, CommandRequestMessage message) {
		root.addProperty("kind", "command_request");
		root.addProperty("commandId", message.commandId().toString());
		root.addProperty("commandType", message.commandType());
		root.add("payload", GSON.toJsonTree(message.payload()));
		root.addProperty("expectedObjectRevision", message.expectedObjectRevision());
		root.addProperty("expectedStateRevision", message.expectedStateRevision().value());
		root.addProperty("actor", message.actor().id());
		root.addProperty("timestamp", message.timestamp().toString());
	}

	private static CommandRequestMessage readCommandRequest(JsonObject root, int version, ArchitecturalObjectId objectId) {
		@SuppressWarnings("unchecked") Map<String, String> payload = GSON.fromJson(root.getAsJsonObject("payload"), Map.class);
		return new CommandRequestMessage(version, objectId, UUID.fromString(requiredString(root, "commandId")),
			requiredString(root, "commandType"), payload, root.get("expectedObjectRevision").getAsLong(),
			new StateRevision(root.get("expectedStateRevision").getAsLong()), new ActorRef(requiredString(root, "actor")),
			Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeCommandAccepted(JsonObject root, CommandAcceptedMessage message) {
		root.addProperty("kind", "command_accepted"); root.addProperty("commandId", message.commandId().toString());
		root.addProperty("resultingObjectRevision", message.resultingObjectRevision());
		root.addProperty("resultingStateRevision", message.resultingStateRevision().value());
		root.addProperty("timestamp", message.timestamp().toString());
	}

	private static CommandAcceptedMessage readCommandAccepted(JsonObject root, int version, ArchitecturalObjectId objectId) {
		return new CommandAcceptedMessage(version, objectId, UUID.fromString(requiredString(root, "commandId")),
			root.get("resultingObjectRevision").getAsLong(), new StateRevision(root.get("resultingStateRevision").getAsLong()),
			Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeCommandRejected(JsonObject root, CommandRejectedMessage message) {
		root.addProperty("kind", "command_rejected"); root.addProperty("commandId", message.commandId().toString());
		root.addProperty("errorCode", message.errorCode().name()); root.addProperty("message", message.message());
		root.addProperty("authoritativeObjectRevision", message.authoritativeObjectRevision());
		root.addProperty("authoritativeStateRevision", message.authoritativeStateRevision().value());
		root.addProperty("timestamp", message.timestamp().toString());
	}

	private static CommandRejectedMessage readCommandRejected(JsonObject root, int version, ArchitecturalObjectId objectId) {
		return new CommandRejectedMessage(version, objectId, UUID.fromString(requiredString(root, "commandId")),
			CommandRejectedMessage.ErrorCode.valueOf(requiredString(root, "errorCode")), requiredString(root, "message"),
			root.get("authoritativeObjectRevision").getAsLong(), new StateRevision(root.get("authoritativeStateRevision").getAsLong()),
			Instant.parse(requiredString(root, "timestamp")));
	}

	private static void writeResyncRequest(JsonObject root, ObjectResyncRequest message) {
		root.addProperty("kind", "resync_request"); root.addProperty("observedObjectRevision", message.observedObjectRevision());
		root.addProperty("observedStateRevision", message.observedStateRevision().value());
		root.addProperty("observedEventSequence", message.observedEventSequence()); root.addProperty("reason", message.reason());
	}

	private static ObjectResyncRequest readResyncRequest(JsonObject root, int version, ArchitecturalObjectId objectId) {
		return new ObjectResyncRequest(version, objectId, root.get("observedObjectRevision").getAsLong(),
			new StateRevision(root.get("observedStateRevision").getAsLong()), root.get("observedEventSequence").getAsLong(),
			requiredString(root, "reason"));
	}
	private static JsonObject writeEvent(ReplicatedEvent event) {
		JsonObject json = new JsonObject();
		json.addProperty("type", event.eventType());
		switch (event) {
			case CommandCommittedReplicationEvent committed -> {
				json.addProperty("commandId", committed.commandId().toString());
				json.addProperty("resultingObjectRevision", committed.resultingObjectRevision());
			}
			case StateTransitionReplicationEvent transition -> {
				json.addProperty("previousRevision", transition.previousRevision().value());
				json.addProperty("currentRevision", transition.currentRevision().value());
				json.add("changedProperties", GSON.toJsonTree(transition.changedProperties()));
			}
		}
		return json;
	}

	private static ReplicatedEvent readEvent(JsonObject json) {
		return switch (requiredString(json, "type")) {
			case "command_committed" -> new CommandCommittedReplicationEvent(
				UUID.fromString(requiredString(json, "commandId")), json.get("resultingObjectRevision").getAsLong());
			case "state_transition" -> new StateTransitionReplicationEvent(
				new StateRevision(json.get("previousRevision").getAsLong()),
				new StateRevision(json.get("currentRevision").getAsLong()),
				Set.copyOf(java.util.Arrays.asList(GSON.fromJson(json.get("changedProperties"), String[].class))));
			default -> throw new IllegalArgumentException("Unknown replicated event type");
		};
	}

	private static JsonObject writeState(Map<String, StateValue> state) {
		JsonObject result = new JsonObject();
		state.forEach((name, value) -> {
			JsonObject json = new JsonObject();
			json.addProperty("type", value.type().name());
			switch (value) {
				case StateValue.BooleanValue v -> json.addProperty("value", v.value());
				case StateValue.NumberValue v -> json.addProperty("value", v.value());
				case StateValue.EnumValue v -> json.addProperty("value", v.value());
				case StateValue.StringValue v -> json.addProperty("value", v.value());
				case StateValue.ReferenceValue v -> json.addProperty("value", v.value());
				case StateValue.VectorValue v -> json.add("value", writeVector(v.value()));
				case StateValue.TransformValue v -> json.add("value", writeTransform(v.value()));
				case StateValue.TimestampValue v -> json.addProperty("value", v.value().toString());
			}
			result.add(name, json);
		});
		return result;
	}

	private static Map<String, StateValue> readState(JsonObject object) {
		Map<String, StateValue> result = new LinkedHashMap<>();
		object.entrySet().forEach(entry -> {
			JsonObject value = entry.getValue().getAsJsonObject();
			JsonElement raw = value.get("value");
			StateValue decoded = switch (StateValueType.valueOf(requiredString(value, "type"))) {
				case BOOLEAN -> StateValue.bool(raw.getAsBoolean());
				case NUMBER -> StateValue.number(raw.getAsDouble());
				case ENUM -> StateValue.enumeration(raw.getAsString());
				case STRING -> StateValue.string(raw.getAsString());
				case REFERENCE -> StateValue.reference(raw.getAsString());
				case VECTOR -> StateValue.vector(readVector(raw.getAsJsonObject()));
				case TRANSFORM -> StateValue.transform(readTransform(raw.getAsJsonObject()));
				case TIMESTAMP -> StateValue.timestamp(Instant.parse(raw.getAsString()));
			};
			result.put(entry.getKey(), decoded);
		});
		return Map.copyOf(result);
	}

	private static JsonObject writeVector(Vec3d value) {
		JsonObject json = new JsonObject();
		json.addProperty("x", value.x()); json.addProperty("y", value.y()); json.addProperty("z", value.z());
		return json;
	}

	private static Vec3d readVector(JsonObject value) {
		return new Vec3d(value.get("x").getAsDouble(), value.get("y").getAsDouble(), value.get("z").getAsDouble());
	}

	private static JsonObject writeTransform(Transform3d value) {
		JsonObject json = new JsonObject();
		json.add("origin", writeVector(value.origin()));
		json.addProperty("facing", value.facing().name());
		json.add("axisOrigin", writeVector(value.rotationAxisOrigin()));
		json.add("axisDirection", writeVector(value.rotationAxisDirection()));
		json.addProperty("radians", value.rotationRadians());
		return json;
	}

	private static Transform3d readTransform(JsonObject value) {
		return new Transform3d(readVector(value.getAsJsonObject("origin")),
			dev.aperture.math.Facing.valueOf(requiredString(value, "facing")),
			readVector(value.getAsJsonObject("axisOrigin")), readVector(value.getAsJsonObject("axisDirection")),
			value.get("radians").getAsDouble());
	}

	private static String requiredString(JsonObject object, String name) {
		JsonElement value = object.get(name);
		if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException("Missing field: " + name);
		return value.getAsString();
	}
}
