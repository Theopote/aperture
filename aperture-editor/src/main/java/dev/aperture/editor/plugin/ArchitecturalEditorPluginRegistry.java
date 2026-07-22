package dev.aperture.editor.plugin;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.interaction.ManipulatorDescriptorProvider;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.read.ObjectEditorView;

import java.util.*;

/** Duplicate-safe ServiceLoader composition root for family editor contributions. */
public final class ArchitecturalEditorPluginRegistry {
	private final List<ArchitecturalEditorPlugin> plugins;

	public ArchitecturalEditorPluginRegistry(List<ArchitecturalEditorPlugin> plugins) {
		Map<String, ArchitecturalEditorPlugin> byId = new LinkedHashMap<>();
		for (ArchitecturalEditorPlugin plugin : plugins) {
			if (plugin.id() == null || plugin.id().isBlank()) throw new IllegalArgumentException("Editor plugin ID is required");
			if (byId.putIfAbsent(plugin.id(), plugin) != null) {
				throw new IllegalArgumentException("Duplicate architectural editor plugin: " + plugin.id());
			}
		}
		this.plugins = List.copyOf(byId.values());
	}

	public static ArchitecturalEditorPluginRegistry discover() {
		return new ArchitecturalEditorPluginRegistry(ServiceLoader.load(ArchitecturalEditorPlugin.class).stream()
			.map(ServiceLoader.Provider::get).toList());
	}

	public ManipulatorDescriptorProvider manipulatorProvider(InspectorModel inspector) {
		return view -> descriptors(view, inspector);
	}

	public List<ManipulatorDescriptor> descriptors(ObjectEditorView view, InspectorModel inspector) {
		ArchitecturalEditorPlugin match = null;
		for (ArchitecturalEditorPlugin plugin : plugins) {
			if (!plugin.supports(view)) continue;
			if (match != null) throw new IllegalStateException("Multiple editor plugins support " + view.typeId());
			match = plugin;
		}
		if (match == null) return List.of();
		List<ManipulatorDescriptor> descriptors = List.copyOf(match.manipulators(view, inspector));
		Set<String> ids = new HashSet<>();
		for (ManipulatorDescriptor descriptor : descriptors) {
			if (!ids.add(descriptor.id())) throw new IllegalStateException("Duplicate manipulator ID: " + descriptor.id());
			var value = view.parameters().get(descriptor.parameterKey()).orElseThrow(() ->
				new IllegalStateException("Manipulator " + descriptor.id() + " references unknown parameter " + descriptor.parameterKey()));
			if (value.type() != descriptor.unit()) throw new IllegalStateException("Manipulator " + descriptor.id()
				+ " expects " + descriptor.unit() + " but parameter is " + value.type());
		}
		return descriptors;
	}

	public List<ArchitecturalEditorPlugin> plugins() { return plugins; }
}
