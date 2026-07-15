package dev.aperture.editor.service;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.manipulation.MirrorAxis;
import dev.aperture.core.editor.manipulation.ResizeAxis;
import dev.aperture.core.editor.session.EditorSession;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;

/**
 * Public API for headless opening editing — manipulators, history, undo/redo.
 */
public final class EditorService {
	public EditorSession openSession(OpeningTypeRegistry openingTypes) {
		return new EditorSession(openingTypes);
	}

	public EditorSession sessionWithInstance(OpeningTypeRegistry openingTypes, OpeningInstance instance) {
		EditorSession session = openSession(openingTypes);
		session.addObject(instance);
		session.select(dev.aperture.core.editor.EditorObjectId.fromInstanceId(instance.instanceId()));
		return session;
	}
}
