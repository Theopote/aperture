package dev.aperture.core.component;

import java.util.LinkedHashMap;
import java.util.Map;

final class ComponentProperties {
	private ComponentProperties() {
	}

	static Map<String, String> copyOf(Map<String, String> properties) {
		return Map.copyOf(properties);
	}

	static Map<String, String> of(String key, String value) {
		return Map.of(key, value);
	}

	static Map<String, String> builder(String... keyValues) {
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("Expected key/value pairs");
		}
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return Map.copyOf(map);
	}
}
