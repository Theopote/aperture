package dev.aperture.core.editor;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.editor.manipulation.MirrorAxis;
import dev.aperture.core.editor.manipulation.ResizeAxis;
import dev.aperture.core.editor.operation.EditorOperations;
import dev.aperture.core.editor.session.EditorSession;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorSessionTest {
	private OpeningTypeRegistry openingTypes;
	private EditorSession session;
	private EditorObjectId doorId;

	@BeforeEach
	void setUp() {
		openingTypes = new OpeningTypeRegistry();
		openingTypes.register(BuiltinOpeningTypes.door());
		session = new EditorSession(openingTypes);
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID)
			.parameters(ParameterSet.empty())
			.build();
		doorId = session.addObject(instance).id();
		session.select(doorId);
	}

	@Test
	void exposesStandardManipulatorsForDoor() {
		var object = session.object(doorId).orElseThrow();
		assertTrue(object.manipulators().stream().anyMatch(m -> m.kind().name().equals("TRANSLATE")));
		assertTrue(object.manipulators().stream().anyMatch(m -> m.kind().name().equals("ROTATE")));
		assertEquals(3, object.resizeHandles().size());
	}

	@Test
	void resizeWidthUpdatesParameterWithUndo() {
		double before = EditorOperations.currentLength(session.object(doorId).orElseThrow(), "width");
		assertTrue(session.resizeAxis(doorId, ResizeAxis.WIDTH, 200).success());
		double after = EditorOperations.currentLength(session.object(doorId).orElseThrow(), "width");
		assertEquals(before + 200, after, 0.01);

		assertTrue(session.undo().success());
		assertEquals(before, EditorOperations.currentLength(session.object(doorId).orElseThrow(), "width"), 0.01);

		assertTrue(session.redo().success());
		assertEquals(after, EditorOperations.currentLength(session.object(doorId).orElseThrow(), "width"), 0.01);
	}

	@Test
	void rotateAndMirrorAreUndoable() {
		var beforeFacing = session.object(doorId).orElseThrow().instance().transform().facing();
		assertTrue(session.rotate(doorId, 90).success());
		assertTrue(session.undo().success());
		assertEquals(beforeFacing, session.object(doorId).orElseThrow().instance().transform().facing());

		assertTrue(session.mirror(doorId, MirrorAxis.X).success());
		assertTrue(session.undo().success());
	}

	@Test
	void copyAddsSecondObject() {
		int before = session.objects().size();
		assertTrue(session.copy(doorId, new dev.aperture.math.Vec3d(500, 0, 0)).success());
		assertEquals(before + 1, session.objects().size());
		assertTrue(session.undo().success());
		assertEquals(before, session.objects().size());
	}
}
