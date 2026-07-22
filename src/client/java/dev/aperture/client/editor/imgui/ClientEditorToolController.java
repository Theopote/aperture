package dev.aperture.client.editor.imgui;

import dev.aperture.client.editor.ClientEditorWorkspace;
import dev.aperture.client.editor.GizmoDragController;
import dev.aperture.client.editor.ResizeTool;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.interaction.ToolManager;
import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.editor.model.selection.SelectionModel;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import net.minecraft.client.Minecraft;

/** Connects product-UI tool intents to the Minecraft world toolchain. */
final class ClientEditorToolController implements ToolController, ClientEditorWorkspace.WorkspaceTools {
	private final SelectionModel selection;
	private final PreviewCoordinator previews;
	private EditorSession session;
	private ResizeTool resizeTool;
	private ToolManager toolManager;
	private Tool active = Tool.SELECT;

	ClientEditorToolController(SelectionModel selection, PreviewCoordinator previews) {
		this.selection = selection;
		this.previews = previews;
	}

	void bind(EditorSession editorSession) {
		this.session = editorSession;
		this.resizeTool = new ResizeTool(editorSession);
		this.toolManager = new ToolManager(resizeTool);
	}

	@Override public Tool activeTool() { return active; }

	@Override
	public boolean activate(Tool tool) {
		if (!available(tool)) return false;
		if (tool == Tool.SELECT) {
			cancelActiveTool();
			return true;
		}
		if (tool == Tool.RESIZE && (toolManager == null || !toolManager.activate(tool))) return false;
		if (tool != Tool.RESIZE && toolManager != null) toolManager.cancelActive();
		active = tool;
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			if (client.screen instanceof ApertureImGuiScreen) client.setScreen(null);
		});
		return true;
	}

	@Override
	public boolean available(Tool tool) {
		return switch (tool) {
			case SELECT, PLACE -> true;
			case ROTATE -> ClientPlacementPreview.session().isPresent();
			case RESIZE -> java.util.Optional.ofNullable(session).flatMap(current -> {
				var primary = current.selection().snapshot().primaryObject();
				return primary == null ? java.util.Optional.empty() : current.readModel().object(primary);
			}).flatMap(view -> view.parameters().get("width")).isPresent();
			case MOVE, ATTACH, MEASURE -> false;
		};
	}

	@Override
	public String disabledReason(Tool tool) {
		return switch (tool) {
			case ROTATE -> "Aim at a valid host to create a placement preview first";
			case RESIZE -> "Select one object with a Width parameter first";
			case MOVE -> "World translation is not connected yet";
			case ATTACH -> "Host reassignment is not connected yet";
			case MEASURE -> "Measurement overlays are not connected yet";
			default -> "Tool unavailable";
		};
	}

	@Override
	public String hint() {
		return switch (active) {
			case PLACE -> "Aim at a host   |   P to place   |   F4 to reopen Aperture";
			case RESIZE -> "Drag a yellow handle   |   Shift: fine   |   Ctrl: no snap   |   Esc: cancel";
			case ROTATE -> "Click the green rotation gizmo   |   P to place   |   Esc to cancel";
			default -> "Click to select   |   Esc to cancel";
		};
	}

	@Override
	public void cancelActiveTool() {
		active = Tool.SELECT;
		if (toolManager != null) toolManager.cancelActive();
		var selected = selection.snapshot().primaryObject();
		if (selected != null) previews.clearObject(selected);
		GizmoDragController.reset();
	}

	@Override public void update(EditorInputFrame input) {
		if (toolManager != null) toolManager.update(input);
	}

	@Override public void cancelTools() { cancelActiveTool(); }

	@Override public ClientEditorWorkspace.ResizeState resizeState() {
		return resizeTool == null ? new ClientEditorWorkspace.ResizeState(false, false)
			: new ClientEditorWorkspace.ResizeState(resizeTool.hovered(), resizeTool.dragging());
	}
}