package dev.aperture.editor.imgui;

import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import imgui.ImGui;

/** Presents command failures on the property that produced them. */
final class InspectorDiagnosticPresenter {
	private InspectorDiagnosticPresenter() { }

	static void render(ObjectEditorView view, PropertyDescriptor property) {
		view.diagnostics().stream()
			.filter(diagnostic -> !diagnostic.resolved())
			.filter(diagnostic -> diagnostic.path().filter(property.key()::equals).isPresent())
			.forEach(diagnostic -> {
				String state = diagnostic.code().contains("conflict") ? "Conflict" : "Rejected";
				ImGui.textColored(ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1, state);
				ImGui.textColored(ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1,
					diagnostic.message());
				if (!diagnostic.suggestedAction().isBlank()) ImGui.textDisabled(diagnostic.suggestedAction());
			});
	}
}
