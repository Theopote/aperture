package dev.aperture.core.object;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;

import java.util.UUID;

/**
 * Universal runtime identity shared by objects placed and operated by Aperture.
 * <p>
 * This contract deliberately contains only the stable state needed by editors,
 * persistence, synchronization, and simulation infrastructure. Family-specific
 * definition identifiers, operational state, hosting rules, and generation
 * pipelines remain on concrete object types.
 * <p>
 * {@code OpeningInstance} is the first implementation; future structural,
 * spatial, equipment, and system objects can participate in the runtime without
 * pretending to be openings.
 */
public interface ArchitecturalObject {
	int schemaVersion();

	UUID instanceId();

	ParameterSet parameters();

	Transform3d transform();

	long revision();
}
