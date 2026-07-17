package dev.aperture.core.serialization;

import java.util.Optional;

/** Resolves assets referenced while decoding definitions or instances. */
@FunctionalInterface
public interface AssetResolver {
	Optional<?> resolve(String assetId);

	static AssetResolver empty() {
		return ignored -> Optional.empty();
	}
}
