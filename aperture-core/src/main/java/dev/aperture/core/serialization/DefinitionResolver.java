package dev.aperture.core.serialization;

import java.util.Optional;

/** Resolves immutable runtime definitions without coupling codecs to a concrete registry. */
@FunctionalInterface
public interface DefinitionResolver {
	Optional<?> resolve(String definitionId);

	default <T> T require(String definitionId, Class<T> expectedType) {
		Object definition = resolve(definitionId)
			.orElseThrow(() -> new IllegalArgumentException("Unknown definition: " + definitionId));
		if (!expectedType.isInstance(definition)) {
			throw new IllegalArgumentException(
				"Definition " + definitionId + " is " + definition.getClass().getName()
					+ ", expected " + expectedType.getName()
			);
		}
		return expectedType.cast(definition);
	}

	static DefinitionResolver empty() {
		return ignored -> Optional.empty();
	}
}
