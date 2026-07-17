package dev.aperture.runtime.model.replication;

/** Canonical wire codec supplied independently from a platform transport. */
public interface ReplicationMessageCodec {
	String encode(ReplicationMessage message);

	ReplicationMessage decode(String encoded);
}
