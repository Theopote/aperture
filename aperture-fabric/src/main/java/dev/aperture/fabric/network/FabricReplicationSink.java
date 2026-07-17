package dev.aperture.fabric.network;

import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.model.replication.ReplicationMessageCodec;
import dev.aperture.runtime.model.replication.ReplicationSink;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Sends authoritative runtime messages to one connected Minecraft client. */
public final class FabricReplicationSink implements ReplicationSink {
	private final ServerPlayer player;
	private final ReplicationMessageCodec codec;

	public FabricReplicationSink(ServerPlayer player, ReplicationMessageCodec codec) {
		this.player = Objects.requireNonNull(player, "player");
		this.codec = Objects.requireNonNull(codec, "codec");
	}

	@Override
	public void publish(ReplicationMessage message) {
		if (!ServerPlayNetworking.canSend(player, ApertureReplicationPayload.TYPE)) return;
		ServerPlayNetworking.send(player, new ApertureReplicationPayload(codec.encode(message)));
	}
}
