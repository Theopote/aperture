package dev.aperture.core.opening;

/**
 * Identifier for a procedural geometry generator binding.
 */
public record GeneratorId(OpeningId id) {
	public static GeneratorId parse(String raw) {
		return new GeneratorId(OpeningId.parse(raw));
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
