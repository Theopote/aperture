package dev.aperture.core.parametric;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametricEditorTest {
	@Test
	void doorSnapshotUsesPlainValuesForExternalTools() {
		var definition = BuiltinOpeningTypes.door();
		var editor = ParametricEditor.fromDefinition(definition, ParameterSet.empty());

		Map<String, Object> snapshot = editor.snapshot();

		assertEquals(1200.0, snapshot.get("width"));
		assertEquals(2300.0, snapshot.get("height"));
		assertEquals(60.0, snapshot.get("thickness"));
		assertEquals(2, snapshot.get("panel_count"));
		assertEquals(0.35, snapshot.get("glass_ratio"));
		assertEquals("left", snapshot.get("hinge_side"));
		assertEquals(false, snapshot.get("has_transom"));
	}

	@Test
	void aiPatchUpdatesParametersWithoutJavaObjects() {
		var definition = BuiltinOpeningTypes.door();
		var editor = ParametricEditor.fromDefinition(definition, ParameterSet.empty());

		ParametricEditResult result = editor.patch(Map.of(
			"width", 1400,
			"height", 2400,
			"panel_count", 4,
			"glass_ratio", 0.5,
			"hinge_side", "right",
			"has_transom", true
		));

		assertTrue(result.success());
		assertEquals(1400.0, external(editor.resolved("width")));
		assertEquals(2400.0, external(editor.resolved("height")));
		assertEquals(4, external(editor.resolved("panel_count")));
		assertEquals(0.5, external(editor.resolved("glass_ratio")));
		assertEquals("right", external(editor.resolved("hinge_side")));
		assertEquals(true, external(editor.resolved("has_transom")));
	}

	@Test
	void rejectsOutOfRangeValues() {
		var definition = BuiltinOpeningTypes.door();
		var editor = ParametricEditor.fromDefinition(definition, ParameterSet.empty());

		ParametricEditResult result = editor.patch(Map.of("width", 100));

		assertFalse(result.success());
	}

	@Test
	void doorJsonRoundTripsThroughReader() throws Exception {
		var reader = new dev.aperture.core.serialization.OpeningTypeDefinitionReader();
		var definition = reader.read(getClass().getClassLoader()
			.getResource("opening_types/door.json")
			.openStream());

		assertEquals(BuiltinOpeningTypes.DOOR_ID, definition.id());
		assertTrue(definition.parametricSchema().get("glass_ratio").orElseThrow() instanceof RangeParameter);
		assertTrue(definition.parametricSchema().get("hinge_side").orElseThrow() instanceof ChoiceParameter);
	}

	@Test
	void invalidDoorParameterPatchIsRejected() {
		var definition = BuiltinOpeningTypes.door();
		var editor = ParametricEditor.fromDefinition(definition, ParameterSet.empty());

		assertTrue(editor.validate(definition).isValid());

		var result = editor.patch(Map.of("glass_ratio", 1.5));
		assertFalse(result.success());
		assertTrue(editor.validate(definition).isValid());
	}

	private static Object external(ParameterValue value) {
		return ParameterBridge.toExternalValue(value);
	}
}
