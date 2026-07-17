package dev.aperture.runtime.model.object;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchitecturalObjectInstanceTest {
	@Test
	void ownsImmutableDurableDataAndAdvancesRevisionExplicitly() {
		List<HostBinding> hosts = new ArrayList<>();
		Map<String, Object> state = new HashMap<>(Map.of("locked", false));
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1,
			ArchitecturalObjectId.random(),
			ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(),
			Transform3d.identity(),
			hosts,
			state,
			4,
			Map.of("source", "test")
		);

		hosts.clear();
		state.put("locked", true);
		assertEquals(List.of(), instance.hostBindings());
		assertEquals(false, instance.persistentState().get("locked"));
		assertEquals(5, instance.withRevision(5).revision());
		assertThrows(UnsupportedOperationException.class,
			() -> instance.persistentState().put("enabled", true));
	}

	@Test
	void rejectsInvalidIdentityAndRevision() {
		assertThrows(IllegalArgumentException.class, () -> ArchitecturalTypeId.parse("Door"));
		assertThrows(IllegalArgumentException.class, () -> new ArchitecturalFamilyId("opening"));
		assertThrows(IllegalArgumentException.class, () -> new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), -1, Map.of()
		));
	}
}
