package dev.aperture.runtime.model.event;

import java.util.Objects;

public record WeatherChangedEvent(String previousWeather, String currentWeather) implements ArchitecturalEvent {
	public WeatherChangedEvent {
		Objects.requireNonNull(previousWeather, "previousWeather");
		Objects.requireNonNull(currentWeather, "currentWeather");
	}
}
