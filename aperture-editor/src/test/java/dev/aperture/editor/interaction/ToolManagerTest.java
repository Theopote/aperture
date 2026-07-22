package dev.aperture.editor.interaction;

import dev.aperture.editor.model.session.ToolController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolManagerTest {
	@Test
	void switchingToolsCancelsAndDeactivatesThePreviousInstance() {
		List<String> events = new ArrayList<>();
		RecordingTool resize = new RecordingTool(ToolController.Tool.RESIZE, events);
		RecordingTool move = new RecordingTool(ToolController.Tool.MOVE, events);
		ToolManager manager = new ToolManager(resize, move);

		assertTrue(manager.activate(ToolController.Tool.RESIZE));
		assertTrue(manager.activate(ToolController.Tool.MOVE));

		assertEquals(List.of("RESIZE:activate", "RESIZE:cancel", "RESIZE:deactivate", "MOVE:activate"), events);
	}

	@Test
	void blockedWorldInputNeverReachesTheActiveTool() {
		RecordingTool resize = new RecordingTool(ToolController.Tool.RESIZE, new ArrayList<>());
		ToolManager manager = new ToolManager(resize);
		manager.activate(ToolController.Tool.RESIZE);

		manager.update(EditorInputFrame.idle());

		assertEquals(0, resize.updates);
	}

	@Test
	void imguiMouseCaptureSuppressesWorldToolEvenWithAWorldRay() {
		RecordingTool resize = new RecordingTool(ToolController.Tool.RESIZE, new ArrayList<>());
		ToolManager manager = new ToolManager(resize);
		manager.activate(ToolController.Tool.RESIZE);
		var ray = new WorldRay(dev.aperture.math.Vec3d.ZERO, new dev.aperture.math.Vec3d(0, 0, 1));
		manager.update(new EditorInputFrame(true, true, false, false, false, false, false,
			true, false, true, new ScreenPoint(100, 100), ray));

		assertEquals(0, resize.updates);
	}
	@Test
	void cancelActiveIsIdempotent() {
		List<String> events = new ArrayList<>();
		ToolManager manager = new ToolManager(new RecordingTool(ToolController.Tool.RESIZE, events));
		manager.activate(ToolController.Tool.RESIZE);
		manager.cancelActive();
		manager.cancelActive();

		assertEquals(List.of("RESIZE:activate", "RESIZE:cancel", "RESIZE:deactivate"), events);
		assertTrue(manager.activeTool().isEmpty());
	}

	private static final class RecordingTool implements EditorTool {
		private final ToolController.Tool id;
		private final List<String> events;
		private int updates;

		private RecordingTool(ToolController.Tool id, List<String> events) {
			this.id = id;
			this.events = events;
		}

		@Override public ToolController.Tool id() { return id; }
		@Override public void activate() { events.add(id + ":activate"); }
		@Override public void update(EditorInputFrame input) { updates++; }
		@Override public void cancel() { events.add(id + ":cancel"); }
		@Override public void deactivate() { events.add(id + ":deactivate"); }
	}
}
