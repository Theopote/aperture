package dev.aperture.runtime.replication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.runtime.model.replication.*;
import java.util.Objects;
import java.time.Instant;

/** Allow-listed JSON codec for bidirectional authority request/response messages. */
public final class JsonAuthorityProtocolCodec {
 private static final Gson GSON=new GsonBuilder()
  .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>)(value,type,context) -> new com.google.gson.JsonPrimitive(value.toString()))
  .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>)(json,type,context) -> Instant.parse(json.getAsString()))
  .create();
 public String encode(ReplicationMessage message) {
  Objects.requireNonNull(message); JsonObject root=new JsonObject();
  String kind=switch(message) {
   case CommandRequestMessage ignored -> "command_request";
   case CommandAcceptedMessage ignored -> "command_accepted";
   case CommandRejectedMessage ignored -> "command_rejected";
   case ObjectResyncRequest ignored -> "resync_request";
   default -> throw new IllegalArgumentException("Not an authority protocol message: "+message.getClass().getName());
  };
  root.addProperty("kind",kind); root.add("payload",GSON.toJsonTree(message)); return GSON.toJson(root);
 }
 public ReplicationMessage decode(String encoded) {
  JsonObject root=JsonParser.parseString(encoded).getAsJsonObject(); String kind=root.get("kind").getAsString();
  return switch(kind) {
   case "command_request" -> GSON.fromJson(root.get("payload"),CommandRequestMessage.class);
   case "command_accepted" -> GSON.fromJson(root.get("payload"),CommandAcceptedMessage.class);
   case "command_rejected" -> GSON.fromJson(root.get("payload"),CommandRejectedMessage.class);
   case "resync_request" -> GSON.fromJson(root.get("payload"),ObjectResyncRequest.class);
   default -> throw new IllegalArgumentException("Unknown authority protocol kind: "+kind);
  };
 }
}