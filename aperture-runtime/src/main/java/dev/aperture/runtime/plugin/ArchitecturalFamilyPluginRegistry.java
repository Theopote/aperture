package dev.aperture.runtime.plugin;

import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.lifecycle.RuntimeObjectConfigurationResolver;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.action.RuntimeActionProjection;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/** Immutable, duplicate-safe family plugin composition root. */
public final class ArchitecturalFamilyPluginRegistry implements RuntimeObjectConfigurationResolver {
	private final List<ArchitecturalFamilyPlugin> plugins;

	public ArchitecturalFamilyPluginRegistry(List<ArchitecturalFamilyPlugin> plugins) {
		Map<String, ArchitecturalFamilyPlugin> byId = new LinkedHashMap<>();
		for (ArchitecturalFamilyPlugin plugin : plugins) {
			if (byId.putIfAbsent(plugin.id(), plugin) != null) {
				throw new IllegalArgumentException("Duplicate architectural family plugin: " + plugin.id());
			}
		}
		this.plugins = List.copyOf(byId.values());
	}

	/** Discovers family contributions without making a platform composition root know concrete families. */
	public static ArchitecturalFamilyPluginRegistry discover() {
		return new ArchitecturalFamilyPluginRegistry(ServiceLoader.load(ArchitecturalFamilyPlugin.class).stream()
			.map(ServiceLoader.Provider::get).toList());
	}
	@Override
	public RuntimeObjectConfiguration resolve(ArchitecturalObjectInstance instance) {
		ArchitecturalFamilyPlugin match = matchingPlugin(instance);
		return match == null ? null : match.configuration(instance);

	}

	public List<RuntimeActionProjection> runtimeActions(ArchitecturalObjectInstance instance, RuntimeState state) {
		ArchitecturalFamilyPlugin match = matchingPlugin(instance);
		return match == null ? List.of() : match.runtimeActions(instance, state);
	}

	private ArchitecturalFamilyPlugin matchingPlugin(ArchitecturalObjectInstance instance) {
		ArchitecturalFamilyPlugin match = null;
		for (ArchitecturalFamilyPlugin plugin : plugins) {
			if (!plugin.supports(instance)) continue;
			if (match != null) throw new IllegalStateException("Multiple family plugins support " + instance.typeId());
			match = plugin;
		}
		return match;
	}
	public List<CommandHandler<?>> commandHandlers() {
		return plugins.stream().flatMap(plugin -> plugin.commandHandlers().stream()).toList();
	}

	public List<ArchitecturalFamilyPlugin> plugins() { return plugins; }
}
