package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Complete observable output of one runtime-pipeline execution. */
public record RuntimeResult(
	ArchitecturalObject previous,
	ArchitecturalObject current,
	Set<RuntimeCapability> capabilities,
	List<RuntimeEffect> effects
) {
	public RuntimeResult {
		Objects.requireNonNull(previous, "previous");
		Objects.requireNonNull(current, "current");
		capabilities = Set.copyOf(capabilities);
		effects = List.copyOf(effects);
	}

	public boolean changed() {
		return !previous.equals(current);
	}
}
