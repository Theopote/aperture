package dev.aperture.runtime.plugin;

import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.action.RuntimeActionProjection;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;

import java.util.List;

/** Family-owned contribution to the generic architectural runtime. */
public interface ArchitecturalFamilyPlugin {
	String id();

	boolean supports(ArchitecturalObjectInstance instance);

	RuntimeObjectConfiguration configuration(ArchitecturalObjectInstance instance);

	List<CommandHandler<?>> commandHandlers();

	default List<RuntimeActionProjection> runtimeActions(ArchitecturalObjectInstance instance, RuntimeState state) { return List.of(); }
}
