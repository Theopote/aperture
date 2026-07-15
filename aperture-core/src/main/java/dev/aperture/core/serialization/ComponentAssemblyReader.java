package dev.aperture.core.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.ComponentRef;
import dev.aperture.core.component.DecorationComponent;
import dev.aperture.core.component.DividerComponent;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.GlassComponent;
import dev.aperture.core.component.HeaderComponent;
import dev.aperture.core.component.HardwareComponent;
import dev.aperture.core.component.OpeningComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.component.SillComponent;
import dev.aperture.core.component.TrimComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@link ComponentAssembly} from opening type JSON.
 */
public final class ComponentAssemblyReader {
	public ComponentAssembly read(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return ComponentAssembly.empty();
		}
		if (element.isJsonArray()) {
			return readArray(element.getAsJsonArray());
		}
		if (element.isJsonObject()) {
			return readLegacyObject(element.getAsJsonObject());
		}
		throw new IllegalArgumentException("components must be an array or object");
	}

	private static ComponentAssembly readArray(JsonArray array) {
		List<OpeningComponent> components = new ArrayList<>();
		for (JsonElement element : array) {
			components.add(readComponent(element.getAsJsonObject()));
		}
		return ComponentAssembly.of(components);
	}

	private static ComponentAssembly readLegacyObject(JsonObject object) {
		Map<String, Object> legacy = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			if (entry.getValue().isJsonObject()) {
				Map<String, String> nested = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> nestedEntry : entry.getValue().getAsJsonObject().entrySet()) {
					nested.put(nestedEntry.getKey(), nestedEntry.getValue().getAsString());
				}
				legacy.put(entry.getKey(), nested);
			} else {
				legacy.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		return ComponentAssembly.fromLegacyMap(legacy);
	}

	private static OpeningComponent readComponent(JsonObject object) {
		String kindKey = object.has("kind") ? object.get("kind").getAsString() : object.get("type").getAsString();
		ComponentKind kind = ComponentKind.fromJsonKey(kindKey);
		String id = object.has("id") ? object.get("id").getAsString() : kind.jsonKey();
		Map<String, String> properties = readProperties(object);

		return switch (kind) {
			case FRAME -> new FrameComponent(ComponentRef.of(id), properties);
			case PANEL -> new PanelComponent(ComponentRef.of(id), properties);
			case GLASS -> new GlassComponent(ComponentRef.of(id), properties);
			case HARDWARE -> new HardwareComponent(ComponentRef.of(id), properties);
			case TRIM -> new TrimComponent(ComponentRef.of(id), properties);
			case SILL -> new SillComponent(ComponentRef.of(id), properties);
			case HEADER -> new HeaderComponent(ComponentRef.of(id), properties);
			case DIVIDER -> new DividerComponent(ComponentRef.of(id), properties);
			case DECORATION -> new DecorationComponent(ComponentRef.of(id), properties);
		};
	}

	private static Map<String, String> readProperties(JsonObject object) {
		Map<String, String> properties = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			String key = entry.getKey();
			if (key.equals("kind") || key.equals("type") || key.equals("id")) {
				continue;
			}
			properties.put(key, entry.getValue().getAsString());
		}
		return properties;
	}
}
