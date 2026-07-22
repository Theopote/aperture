package dev.aperture.opening.editor.plugin;

import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.state.RuntimeState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoorManipulatorContractTest {
	@Test
	void widthAndHeightAreDeclaredByTheSamePlugin() {
		var parameters = ParameterSet.builder().put("width", ParameterValue.length(900))
			.put("height", ParameterValue.length(2100)).build();
		var view = new ObjectEditorView(ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), "Door", Transform3d.identity(), List.of(), parameters,
			RuntimeState.initial(dev.aperture.runtime.model.state.StateSchema.builder("door", 1).build()),
			1, 1, dev.aperture.editor.model.read.SyncStatus.SYNCHRONIZED, List.of(), List.of());
		var descriptors = new OpeningArchitecturalEditorPlugin().manipulators(view, objectId -> List.of());

		assertEquals(List.of("door.width.right", "door.height.top"),
			descriptors.stream().map(ManipulatorDescriptor::id).toList());
		var height = descriptors.get(1);
		assertEquals(ManipulatorDescriptor.Axis.LOCAL_Y, height.axis());
		assertEquals(ManipulatorDescriptor.Anchor.TOP_MIDPOINT, height.anchor());
		assertEquals(ManipulatorDescriptor.Anchor.BOTTOM_MIDPOINT, height.fixedAnchor());
	}
}
