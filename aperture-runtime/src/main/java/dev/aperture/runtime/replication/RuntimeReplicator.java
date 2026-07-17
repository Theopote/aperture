package dev.aperture.runtime.replication;

import dev.aperture.core.object.ArchitecturalObject;

/** Platform port for publishing committed revision changes to remote replicas. */
@FunctionalInterface
public interface RuntimeReplicator {
	void replicate(ArchitecturalObject previous, ArchitecturalObject current);

	static RuntimeReplicator noop() {
		return (previous, current) -> { };
	}
}
