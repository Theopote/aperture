package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.CommandEnvelope;
import java.time.Duration;
import java.util.Objects;

public record ScheduleCommandEffect(CommandEnvelope<? extends ArchitecturalCommand> command, Duration delay) implements WorldEffect {
	public ScheduleCommandEffect {
		Objects.requireNonNull(command, "command"); Objects.requireNonNull(delay, "delay");
		if (delay.isNegative()) throw new IllegalArgumentException("delay must not be negative");
	}
	@Override public String effectType() { return "aperture:schedule_command"; }
}
