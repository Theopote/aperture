package dev.aperture.fabric.network;

import dev.aperture.Aperture;
import dev.aperture.fabric.runtime.FabricRuntimeLifecycle;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.model.replication.ObjectResyncRequest;
import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.replication.JsonReplicationMessageCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Bidirectional Fabric envelope around the platform-neutral authority protocol. */
public final class ApertureReplicationNetworking {
	private static final JsonReplicationMessageCodec CODEC = new JsonReplicationMessageCodec();
	private ApertureReplicationNetworking() { }

	public static void registerPayloadType() {
		PayloadTypeRegistry.clientboundPlay().register(ApertureReplicationPayload.TYPE, ApertureReplicationPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ApertureReplicationPayload.TYPE, ApertureReplicationPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ApertureReplicationPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handle(payload, context)));
	}

	private static void handle(ApertureReplicationPayload payload, ServerPlayNetworking.Context context) {
		try {
			ReplicationMessage message = CODEC.decode(payload.encoded());
			if (message instanceof CommandRequestMessage request) {
				var outcome = FabricRuntimeLifecycle.submit(request);
				send(context.player(), outcome.response());
				for (ReplicationMessage update : outcome.broadcasts()) {
					for (var player : context.server().getPlayerList().getPlayers()) send(player, update);
				}
			} else if (message instanceof ObjectResyncRequest request) {
				send(context.player(), FabricRuntimeLifecycle.resync(request));
			} else {
				Aperture.LOGGER.warn("Rejected non-request serverbound Aperture message: {}", message.getClass().getSimpleName());
			}
		} catch (RuntimeException error) {
			Aperture.LOGGER.warn("Rejected malformed Aperture authority payload from {}", context.player().getGameProfile().name(), error);
		}
	}

	private static void send(net.minecraft.server.level.ServerPlayer player, ReplicationMessage message) {
		if (ServerPlayNetworking.canSend(player, ApertureReplicationPayload.TYPE))
			ServerPlayNetworking.send(player, new ApertureReplicationPayload(CODEC.encode(message)));
	}
}