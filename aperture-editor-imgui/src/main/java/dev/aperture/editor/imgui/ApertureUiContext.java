package dev.aperture.editor.imgui;

import dev.aperture.editor.model.session.EditorSession;
import java.util.Objects;

final class ApertureUiContext {
	enum Mode { DESIGN, RUNTIME, ANALYZE }
	final EditorSession session;
	Mode mode = Mode.DESIGN;
	boolean snap = true;
	ApertureUiContext(EditorSession session) { this.session = Objects.requireNonNull(session); }
}
