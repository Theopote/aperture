package dev.aperture.fabric.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static dev.aperture.Aperture.MOD_ID;

/** Minecraft transport envelope for a canonical runtime replication message. */
public record ApertureReplicationPayload(String encoded) implements CustomPacketPayload {
	private static final int MAX_MESSAGE_LENGTH = 1_048_576;

	public static final Type<ApertureReplicationPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MOD_ID, "runtime_replication"));
	public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ApertureReplicationPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.stringUtf8(MAX_MESSAGE_LENGTH),
			ApertureReplicationPayload::encoded, ApertureReplicationPayload::new);

	@Override
	public Type<ApertureReplicationPayload> type() { return TYPE; }
}
