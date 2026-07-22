package dev.aperture.editor.imgui;

import dev.aperture.editor.interaction.DimensionValueParser;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.type.ImString;
import imgui.flag.ImGuiInputTextFlags;
import dev.aperture.parameter.ParameterValue;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

/** Lightweight product HUD anchored to the Minecraft-world dock node. */
final class ViewportOverlay {
	private static final float MARGIN = 12;
	private static final int HUD_FLAGS = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoDocking
		| ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.NoNav;
	private final ApertureUiContext context;
	private final ViewportProductLayers productLayers = new ViewportProductLayers();
	private final DimensionValueParser dimensionParser = new DimensionValueParser();
	private final ImString dimensionInput = new ImString(32);
	private ArchitecturalObjectId dimensionObject;
	private String dimensionParameter;
	private double dimensionBase;
	private long dimensionObjectRevision;
	private long dimensionStateRevision;
	private String dimensionError = "";

	ViewportOverlay(ApertureUiContext context) { this.context = context; }

	void render(int dockspaceId) {
		var node = imgui.internal.ImGui.dockBuilderGetCentralNode(dockspaceId);
		if (node == null || node.getSizeX() < 320 || node.getSizeY() < 180) return;
		float x = node.getPosX();
		float y = node.getPosY();
		float width = node.getSizeX();
		float height = node.getSizeY();
		ViewportOverlayViewModel model = ViewportOverlayViewModel.from(context);
		productLayers.render(model, x, y, width, height);
		viewportToolbar(x + width * .5f - 115, y + MARGIN);
		dimensionInteraction();
	}

	private void dimensionInteraction() {
		var primary = context.session.selection().snapshot().primaryObject();
		if (primary != null) DimensionEditRequests.consume().filter(request -> request.objectId().equals(primary)).ifPresent(request ->
			beginDimension(request.objectId(), request.parameterKey(), request.baseMillimeters(),
				request.objectRevision(), request.stateRevision()));
		dimensionPopup();
	}

	private void viewportToolbar(float x, float y) {
		beginHud("Viewport Tools##Viewport", x, y, 230, 42, false);
		if (ImGui.button("Select##Viewport")) context.session.tools().cancelActiveTool();
		ImGui.sameLine();
		if (ImGui.button(context.snap ? "Snap: On" : "Snap: Off")) context.snap = !context.snap;
		ImGui.sameLine();
		ImGui.beginDisabled();
		try { ImGui.button("World"); } finally { ImGui.endDisabled(); }
		if (ImGui.isItemHovered()) ImGui.setTooltip("Local coordinates are not connected yet");
		ImGui.end();
	}

	private void beginDimension(ArchitecturalObjectId objectId, String parameter, double base,
		long objectRevision, long stateRevision) {
		dimensionObject = objectId;
		dimensionParameter = parameter;
		dimensionBase = base;
		dimensionObjectRevision = objectRevision;
		dimensionStateRevision = stateRevision;
		dimensionInput.set(Long.toString(Math.round(base)));
		dimensionError = "";
		ImGui.openPopup("Edit Dimension");
	}
	private void dimensionPopup() {
		if (dimensionObject == null || !ImGui.beginPopup("Edit Dimension")) return;
		ImGui.text("Set " + dimensionParameter);
		ImGui.textDisabled("Examples: 1350, 1.35m, +100, -50");
		boolean submit = ImGui.inputText("##DimensionValue", dimensionInput, ImGuiInputTextFlags.EnterReturnsTrue);
		if (ImGui.button("Apply")) submit = true;
		ImGui.sameLine();
		if (ImGui.button("Cancel")) {
			ImGui.closeCurrentPopup();
			dimensionError = "";
		}
		if (submit) submitDimension();
		if (!dimensionError.isBlank()) ImGui.textColored(
			ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1, dimensionError);
		ImGui.endPopup();
	}

	private void submitDimension() {
		try {
			double requested = dimensionParser.parse(dimensionInput.get(), dimensionBase).millimeters();
			double constrained = constrainDimension(requested);
			if (Math.abs(constrained - dimensionBase) < .001) {
				ImGui.closeCurrentPopup();
				return;
			}
			var edit = new DefaultParameterEditSession(dimensionObject, dimensionParameter,
				ParameterValue.length(dimensionBase), new ExpectedRevision(dimensionObjectRevision, dimensionStateRevision),
				context.session.preview(), context.session.commands());
			edit.updatePreview(ParameterValue.length(constrained));
			edit.commit();
			dimensionError = "";
			ImGui.closeCurrentPopup();
		} catch (IllegalArgumentException error) {
			dimensionError = error.getMessage();
		}
	}

	private double constrainDimension(double requested) {
		double minimum = 0;
		double maximum = Double.MAX_VALUE;
		for (var section : context.session.inspector().sections(dimensionObject)) for (var property : section.properties()) {
			if (property.key().equals(dimensionParameter)) {
				minimum = property.minimum().orElse(minimum);
				maximum = property.maximum().orElse(maximum);
			}
		}
		if (requested < minimum || requested > maximum) {
			throw new IllegalArgumentException("Value must be between " + Math.round(minimum) + " and "
				+ (Double.isFinite(maximum) && maximum < Double.MAX_VALUE ? Math.round(maximum) : "unlimited") + " mm");
		}
		return requested;
	}
	private static void beginHud(String name, float x, float y, float width, float height, boolean noInputs) {
		ImGui.setNextWindowPos(x, y);
		ImGui.setNextWindowSize(width, height);
		ImGui.setNextWindowBgAlpha(.88f);
		ImGui.begin(name, HUD_FLAGS | (noInputs ? ImGuiWindowFlags.NoInputs : 0));
	}

}
