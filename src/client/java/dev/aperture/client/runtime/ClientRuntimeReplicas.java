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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Client projection store; server messages are its only state input. */
public final class ClientRuntimeReplicas {
	private static final JsonReplicationMessageCodec CODEC = new JsonReplicationMessageCodec();
	private static final ArchitecturalFamilyPluginRegistry FAMILIES =
		new ArchitecturalFamilyPluginRegistry(List.of(new OpeningArchitecturalFamilyPlugin()));
	private static final List<Consumer<ReplicationMessage>> LISTENERS = new CopyOnWriteArrayList<>();
	private static final ClientReplicaStore STORE = new ClientReplicaStore(
		AuthoritativeCommandGateway.PROTOCOL_VERSION,
		instance -> FAMILIES.resolve(instance).stateSchema());

	private ClientRuntimeReplicas() { }

	public static void registerReceiver() {
		ClientPlayNetworking.registerGlobalReceiver(ApertureReplicationPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ReplicationMessage message = CODEC.decode(payload.encoded());
				if (message instanceof dev.aperture.runtime.model.replication.ObjectSnapshotMessage
					|| message instanceof dev.aperture.runtime.model.replication.StateDeltaMessage
					|| message instanceof dev.aperture.runtime.model.replication.EventDeltaMessage
					|| message instanceof dev.aperture.runtime.model.replication.ObjectRemovedMessage) STORE.apply(message);
				LISTENERS.forEach(listener -> listener.accept(message));
			}));
	}

	public static ClientReplicaStore store() { return STORE; }
	public static void addMessageListener(Consumer<ReplicationMessage> listener) { LISTENERS.add(listener); }
	static ClientReplicaStore.ApplyResult apply(ReplicationMessage message) { return STORE.apply(message); }
}
