package dev.aperture.runtime.model.event;

import dev.aperture.runtime.model.object.HostBinding;

import java.util.List;
import java.util.Objects;

public record HostChangedEvent(ObjectRef target, List<HostBinding> previous, List<HostBinding> current)
	implements ArchitecturalEvent {
	public HostChangedEvent {
		Objects.requireNonNull(target, "target");
		previous = List.copyOf(previous);
		current = List.copyOf(current);
	}
}
