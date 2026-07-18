package dev.aperture.fabric.runtime;

import dev.aperture.core.instance.HostAttachmentMode;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;

import java.util.List;
import java.util.Map;

/** One-way migration adapter from the legacy placed Door to the K2 authoritative snapshot. */
public final class OpeningRuntimeSnapshotAdapter {
	private OpeningRuntimeSnapshotAdapter() { }

	public static ArchitecturalObjectSnapshot fromDoor(OpeningInstance opening) {
		if (!opening.typeId().toString().equals("aperture:door")) {
			throw new IllegalArgumentException("K2.2 runtime activation currently supports aperture:door only");
		}
		Map<String, StateValue> persistent = Map.of(
			"openRatio", StateValue.number(opening.state().openRatio()),
			"targetOpenRatio", StateValue.number(opening.state().openRatio()),
			"locked", StateValue.bool(opening.state().locked()),
			"enabled", StateValue.bool(true));
		Map<String, Object> instanceState = new java.util.LinkedHashMap<>();
		instanceState.putAll(persistent);
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			opening.schemaVersion(), new ArchitecturalObjectId(opening.instanceId()),
			ArchitecturalTypeId.parse(opening.typeId().toString()),
			new ArchitecturalFamilyId("aperture:opening"), opening.parameters(), opening.transform(),
			hostBindings(opening), instanceState, opening.revision(), Map.of("migratedFrom", "OpeningInstance"));
		long stateRevision = opening.state().runtimeState().revision();
		return new ArchitecturalObjectSnapshot(1, instance, persistent, new StateRevision(stateRevision),
			opening.state().runtimeState().timestamp(), List.of());
	}

	private static List<dev.aperture.runtime.model.object.HostBinding> hostBindings(OpeningInstance opening) {
		var host = opening.host();
		if (host.mode() == HostAttachmentMode.FREE_STANDING) return List.of();
		Transform3d insertion = new Transform3d(host.insertionFrame().origin(), Facing.NORTH);
		return List.of(new dev.aperture.runtime.model.object.HostBinding(
			new ArchitecturalObjectId(host.hostId().value()), host.featureId().type() + ":" + host.featureId().value(),
			insertion, host.mode().name().toLowerCase(java.util.Locale.ROOT),
			host.attachmentParameters(), host.hostRevision()));
	}
}
