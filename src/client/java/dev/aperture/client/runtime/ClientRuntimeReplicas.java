package dev.aperture.client.runtime;

import dev.aperture.fabric.network.ApertureReplicationPayload;
import dev.aperture.opening.runtime.plugin.OpeningArchitecturalFamilyPlugin;
import dev.aperture.runtime.model.replication.ClientReplicaStore;
import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.plugin.ArchitecturalFamilyPluginRegistry;
import dev.aperture.runtime.replication.AuthoritativeCommandGateway;
import dev.aperture.runtime.replication.JsonReplicationMessageCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

/** Client projection store; server messages are its only state input. */
public final class ClientRuntimeReplicas {
	private static final JsonReplicationMessageCodec CODEC = new JsonReplicationMessageCodec();
	private static final ArchitecturalFamilyPluginRegistry FAMILIES =
		new ArchitecturalFamilyPluginRegistry(List.of(new OpeningArchitecturalFamilyPlugin()));
	private static final ClientReplicaStore STORE = new ClientReplicaStore(
		AuthoritativeCommandGateway.PROTOCOL_VERSION,
		instance -> FAMILIES.resolve(instance).stateSchema());

	private ClientRuntimeReplicas() { }

	public static void registerReceiver() {
		ClientPlayNetworking.registerGlobalReceiver(ApertureReplicationPayload.TYPE, (payload, context) ->
			context.client().execute(() -> STORE.apply(CODEC.decode(payload.encoded()))));
	}

	public static ClientReplicaStore store() { return STORE; }
	static ClientReplicaStore.ApplyResult apply(ReplicationMessage message) { return STORE.apply(message); }
}
