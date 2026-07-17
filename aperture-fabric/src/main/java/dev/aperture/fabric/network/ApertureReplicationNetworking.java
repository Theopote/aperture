package dev.aperture.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registration boundary for Aperture runtime replication packets. */
public final class ApertureReplicationNetworking {
	private ApertureReplicationNetworking() { }

	public static void registerPayloadType() {
		PayloadTypeRegistry.clientboundPlay().register(
			ApertureReplicationPayload.TYPE, ApertureReplicationPayload.CODEC);
	}
}
