package dev.aperture.runtime.mapper;

import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parametric.ParameterBridge;
import dev.aperture.kernel.OpeningRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Maps a persisted world instance to the Kernel request contract. */
public final class OpeningInstanceRequestMapper {
	public OpeningRequest map(OpeningInstance instance) {
		Objects.requireNonNull(instance, "instance cannot be null");
		Map<String, Object> parameters = new LinkedHashMap<>();
		instance.parameters().asMap().forEach((name, value) ->
			parameters.put(name, ParameterBridge.toExternalValue(value))
		);
		return new OpeningRequest(
			instance.typeId().toString(),
			parameters,
			instance.state(),
			dev.aperture.kernel.OpeningOptions.DEFAULT
		);
	}
}