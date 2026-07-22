package dev.aperture.client.editor.imgui;

import dev.aperture.client.editor.GizmoDragController;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.editor.model.selection.SelectionModel;
import dev.aperture.editor.model.session.ToolController;
import net.minecraft.client.Minecraft;

/** Connects product-UI tool intents to the existing Minecraft world toolchain. */
final class ClientEditorToolController implements ToolController {
	private final SelectionModel selection;
	private final PreviewCoordinator previews;

	ClientEditorToolController(SelectionModel selection, PreviewCoordinator previews) {
		this.selection = selection;
		this.previews = previews;
	}
	private Tool active = Tool.SELECT;

	@Override
	public Tool activeTool() { return active; }

	@Override
	public boolean activate(Tool tool) {
		if (!available(tool)) return false;
		if (tool == Tool.SELECT) {
			cancelActiveTool();
			return true;
		}
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
			case ROTATE, RESIZE -> ClientPlacementPreview.session().isPresent();
			case MOVE, ATTACH, MEASURE -> false;
		};
	}

	@Override
	public String disabledReason(Tool tool) {
		return switch (tool) {
			case ROTATE, RESIZE -> "Aim at a valid host to create a placement preview first";
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
			case RESIZE -> "Drag a yellow handle   |   P to place   |   Esc to cancel";
			case ROTATE -> "Click the green rotation gizmo   |   P to place   |   Esc to cancel";
			default -> "Click to select   |   Esc to cancel";
		};
	}

	@Override
	public void cancelActiveTool() {
		active = Tool.SELECT;
		var selected = selection.snapshot().primaryObject();
		if (selected != null) previews.clearObject(selected);
		GizmoDragController.reset();
	}
}
