package dev.aperture.editor.imgui;

import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.interaction.DimensionValueParser;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.type.ImString;
import imgui.flag.ImGuiInputTextFlags;
import dev.aperture.editor.model.read.SyncStatus;
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

	private void modeHud(float x, float y) {
		beginHud("Viewport Mode##Viewport", x, y, 190, 100, true);
		float[] color = context.mode == ApertureUiContext.Mode.RUNTIME ? ApertureStyle.RUNTIME : ApertureStyle.BLUE;
		ImGui.textColored(color[0], color[1], color[2], 1, context.mode + " MODE");
		ImGui.textDisabled("Minecraft World");
		ImGui.text("Tool  " + context.session.tools().activeTool());
		ImGui.text("Snap  " + (context.snap ? "On" : "Off") + "   |   Coordinates  World");
		ImGui.end();
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

	private void selectionHud(float x, float y) {
		beginHud("Selection##Viewport", x, y, 260, 100, true);
		var selection = context.session.selection().snapshot();
		if (selection.objectIds().isEmpty()) {
			ImGui.textDisabled("No object selected");
			ImGui.textDisabled("Click an Aperture object in the world");
		} else if (selection.objectIds().size() > 1) {
			ImGui.text(selection.objectIds().size() + " objects selected");
		} else context.session.readModel().object(selection.primaryObject()).ifPresent(this::selectedObject);
		ImGui.end();
	}

	private void selectedObject(ObjectEditorView view) {
		DimensionEditRequests.consume().filter(request -> request.objectId().equals(view.objectId())).ifPresent(request ->
			beginDimension(request.objectId(), request.parameterKey(), request.baseMillimeters(),
				request.objectRevision(), request.stateRevision()));
		ImGui.text("1 object selected");
		ImGui.text(view.displayName());
		String dimensions = dimensions(view);
		if (!dimensions.isBlank()) ImGui.textDisabled(dimensions);
		dimensionButton(view, "width", "Width");
		ImGui.sameLine();
		dimensionButton(view, "height", "Height");
		dimensionPopup();
		var previews = context.session.preview().values(view.objectId());
		if (!view.diagnostics().isEmpty()) ImGui.textColored(
			ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1,
			view.diagnostics().size() + " issue" + (view.diagnostics().size() == 1 ? "" : "s"));
		else if (!previews.isEmpty()) ImGui.textColored(
			ApertureStyle.BLUE[0], ApertureStyle.BLUE[1], ApertureStyle.BLUE[2], 1,
			"Pending / previewing " + previews.size() + " change" + (previews.size() == 1 ? "" : "s"));
		else if (view.syncStatus() == SyncStatus.RESYNC_REQUIRED) ImGui.textColored(
			ApertureStyle.WARNING[0], ApertureStyle.WARNING[1], ApertureStyle.WARNING[2], 1,
			"Resynchronization required");
	}

	private void dimensionButton(ObjectEditorView view, String parameter, String label) {
		view.parameters().get(parameter).filter(ParameterValue.LengthValue.class::isInstance)
			.map(ParameterValue.LengthValue.class::cast).ifPresent(value -> {
				String caption = label + " " + Math.round(value.millimeters()) + " mm##dimension." + parameter;
				if (ImGui.smallButton(caption)) {
					beginDimension(view.objectId(), parameter, value.millimeters(),
						view.objectRevision(), view.stateRevision());
				}
			});
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
	private void helpHud(float x, float y) {
		beginHud("Tool Help##Viewport", x, y, 380, 36, true);
		String help = context.mode == ApertureUiContext.Mode.RUNTIME
			? "Click an object to inspect state   |   Esc to cancel"
			: context.session.tools().hint();
		ImGui.textDisabled(help);
		ImGui.end();
	}

	private static void beginHud(String name, float x, float y, float width, float height, boolean noInputs) {
		ImGui.setNextWindowPos(x, y);
		ImGui.setNextWindowSize(width, height);
		ImGui.setNextWindowBgAlpha(.88f);
		ImGui.begin(name, HUD_FLAGS | (noInputs ? ImGuiWindowFlags.NoInputs : 0));
	}

	private static String dimensions(ObjectEditorView view) {
		String width = length(view, "width");
		String height = length(view, "height");
		if (!width.isBlank() && !height.isBlank()) return width + " 脳 " + height;
		return !width.isBlank() ? "Width  " + width : !height.isBlank() ? "Height  " + height : "";
	}

	private static String length(ObjectEditorView view, String key) {
		return view.parameters().get(key)
			.filter(ParameterValue.LengthValue.class::isInstance)
			.map(ParameterValue.LengthValue.class::cast)
			.map(value -> Math.round(value.millimeters()) + " mm")
			.orElse("");
	}
}
