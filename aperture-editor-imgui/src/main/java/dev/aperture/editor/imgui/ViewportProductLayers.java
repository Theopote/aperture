package dev.aperture.editor.imgui;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

/** Four stable presentation layers for the world-centric workspace. */
final class ViewportProductLayers {
	private static final int FLAGS = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoDocking
		| ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoFocusOnAppearing
		| ImGuiWindowFlags.NoNav | ImGuiWindowFlags.NoInputs;

	void render(ViewportOverlayViewModel model, float x, float y, float width, float height) {
		mode(model, x + 12, y + 12);
		selection(model, x + width - 272, y + 12);
		help(model, x + width * .5f - 210, y + height - 64);
		operation(model, x + width * .5f - 170, y + height - 136);
	}

	private void mode(ViewportOverlayViewModel model, float x, float y) {
		begin("Viewport Mode##ProductViewport", x, y, 190, 106);
		ImGui.textColored(ApertureStyle.BLUE[0], ApertureStyle.BLUE[1], ApertureStyle.BLUE[2], 1, model.mode());
		ImGui.text(model.tool());
		ImGui.text(model.snap());
		ImGui.textDisabled(model.coordinates());
		ImGui.end();
	}

	private void selection(ViewportOverlayViewModel model, float x, float y) {
		begin("Viewport Selection##ProductViewport", x, y, 260, 106);
		if (model.selection().isEmpty()) {
			ImGui.textDisabled("No object selected");
			ImGui.textDisabled("Select in the world or Project Navigator");
		} else {
			var selected = model.selection().orElseThrow();
			ImGui.text(selected.name());
			ImGui.textDisabled(selected.type());
			if (!selected.dimensions().isBlank()) ImGui.text(selected.dimensions());
			ImGui.textDisabled(selected.sync());
		}
		ImGui.end();
	}

	private void help(ViewportOverlayViewModel model, float x, float y) {
		begin("Viewport Help##ProductViewport", x, y, 420, 52);
		ImGui.text(model.primaryHint());
		ImGui.textDisabled(model.modifierHint());
		ImGui.end();
	}

	private void operation(ViewportOverlayViewModel model, float x, float y) {
		if (model.operation().kind() == ViewportOverlayViewModel.OperationKind.NONE) return;
		begin("Viewport Operation##ProductViewport", x, y, 340, 60);
		float[] color = switch (model.operation().kind()) {
			case PREVIEW, PENDING -> ApertureStyle.BLUE;
			case CONFLICT -> ApertureStyle.WARNING;
			case REJECTED -> ApertureStyle.ERROR;
			case NONE -> ApertureStyle.SUCCESS;
		};
		ImGui.textColored(color[0], color[1], color[2], 1, model.operation().title());
		ImGui.textDisabled(model.operation().detail());
		ImGui.end();
	}

	private static void begin(String name, float x, float y, float width, float height) {
		ImGui.setNextWindowPos(x, y);
		ImGui.setNextWindowSize(width, height);
		ImGui.setNextWindowBgAlpha(.9f);
		ImGui.begin(name, FLAGS);
	}
}
