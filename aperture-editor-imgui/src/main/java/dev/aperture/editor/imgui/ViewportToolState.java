package dev.aperture.editor.imgui;

import dev.aperture.editor.interaction.ToolInteractionState;

import java.util.Optional;

/** Small frontend boundary carrying world-tool lifecycle into the overlay ViewModel. */
public final class ViewportToolState {
	private static volatile Snapshot current = Snapshot.IDLE;
	private ViewportToolState() { }

	public static void publish(ToolInteractionState state, Optional<String> manipulatorId) {
		current = new Snapshot(state, manipulatorId);
	}
	public static Snapshot current() { return current; }
	public static void clear() { current = Snapshot.IDLE; }

	public record Snapshot(ToolInteractionState state, Optional<String> manipulatorId) {
		private static final Snapshot IDLE = new Snapshot(ToolInteractionState.IDLE, Optional.empty());
		public Snapshot {
			state = state == null ? ToolInteractionState.IDLE : state;
			manipulatorId = manipulatorId == null ? Optional.empty() : manipulatorId;
		}
	}
}
