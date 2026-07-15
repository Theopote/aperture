package dev.aperture.client.editor;

import dev.aperture.core.editor.manipulation.ManipulatorKind;

import java.util.Objects;

/**
 * Identifies one interactive gizmo element under the crosshair.
 */
public record GizmoPickTarget(
	Kind kind,
	String id,
	ManipulatorKind manipulatorKind
) {
	public enum Kind {
		RESIZE_HANDLE,
		MANIPULATOR
	}

	public GizmoPickTarget {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(id, "id");
	}

	public static GizmoPickTarget resizeHandle(String handleId) {
		return new GizmoPickTarget(Kind.RESIZE_HANDLE, handleId, ManipulatorKind.RESIZE);
	}

	public static GizmoPickTarget manipulator(String id, ManipulatorKind kind) {
		return new GizmoPickTarget(Kind.MANIPULATOR, id, kind);
	}

	public boolean isResizeHandle() {
		return kind == Kind.RESIZE_HANDLE;
	}
}
