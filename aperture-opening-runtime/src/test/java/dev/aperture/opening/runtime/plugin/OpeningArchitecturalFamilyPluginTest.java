package dev.aperture.opening.runtime.plugin;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.runtime.plugin.ArchitecturalFamilyPluginRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpeningArchitecturalFamilyPluginTest {
	@Test
	void contributesDoorConfigurationAndHandlersWithoutRuntimeKnowingOpening() {
		var registry = new ArchitecturalFamilyPluginRegistry(List.of(new OpeningArchitecturalFamilyPlugin()));
		var configuration = registry.resolve(door());
		assertNotNull(configuration);
		assertTrue(configuration.stateSchema().properties().containsKey("openRatio"));
		assertTrue(registry.commandHandlers().stream()
			.anyMatch(handler -> handler.commandClass() == RequestOpenCommand.class));
	}

	@Test
	void familyOwnsRuntimeActionProjection() {
		var registry = new ArchitecturalFamilyPluginRegistry(List.of(new OpeningArchitecturalFamilyPlugin()));
		var actions = registry.runtimeActions(door(), RuntimeState.initial(DoorStateSchema.SCHEMA));
		assertEquals(List.of("request_open","request_close","set_locked"),actions.stream().map(action->action.id()).toList());
		assertTrue(actions.stream().filter(action->action.id().equals("request_open")).findFirst().orElseThrow().enabled());
	}
	@Test
	void serviceDiscoveryKeepsPlatformRootsFamilyAgnostic() {
		var registry=ArchitecturalFamilyPluginRegistry.discover();
		assertTrue(registry.plugins().stream().anyMatch(plugin->plugin.id().equals("aperture:opening")));
		assertNotNull(registry.resolve(door()));
	}
	private static ArchitecturalObjectInstance door() {
		return new ArchitecturalObjectInstance(1, ArchitecturalObjectId.random(),
			ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(), 0, Map.of());
	}
}
