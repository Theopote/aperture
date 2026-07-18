package dev.aperture.opening.runtime.plugin;

import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.runtime.*;
import dev.aperture.runtime.lifecycle.KinematicModel;
import dev.aperture.runtime.lifecycle.RuntimeObjectConfiguration;
import dev.aperture.runtime.model.command.CommandHandler;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.plugin.ArchitecturalFamilyPlugin;

import java.util.List;

/** Opening-owned registration of Door behavior into the generic runtime. */
public final class OpeningArchitecturalFamilyPlugin implements ArchitecturalFamilyPlugin {
	private static final ArchitecturalTypeId DOOR = ArchitecturalTypeId.parse("aperture:door");
	private final RuntimeObjectConfiguration door = new RuntimeObjectConfiguration(
		DoorStateSchema.SCHEMA, DoorCapabilities::from,
		List.of(new ManualDoorInteractionBehavior()),
		new KinematicModel(List.of(DoorKinematics.swingPanel(
			new ComponentPath("door.panel").value(), Vec3d.ZERO, false))),
		DoorRuntimeTick.atSpeed(1.0));
	private final List<CommandHandler<?>> handlers = List.of(
		new RequestOpenDoorHandler(), new RequestCloseDoorHandler(), new SetDoorLockHandler());

	@Override public String id() { return "aperture:opening"; }
	@Override public boolean supports(ArchitecturalObjectInstance instance) { return instance.typeId().equals(DOOR); }
	@Override public RuntimeObjectConfiguration configuration(ArchitecturalObjectInstance instance) {
		if (!supports(instance)) throw new IllegalArgumentException("Unsupported opening type: " + instance.typeId());
		return door;
	}
	@Override public List<CommandHandler<?>> commandHandlers() { return handlers; }
}
