package dev.aperture.editor.imgui;

import dev.aperture.editor.interaction.ToolInteractionState;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.read.SyncStatus;
import dev.aperture.parameter.ParameterValue;

import java.util.Map;
import java.util.Optional;

/** One immutable product snapshot consumed by all four viewport overlay layers. */
record ViewportOverlayViewModel(
	String mode,
	String tool,
	String snap,
	String coordinates,
	Optional<Selection> selection,
	String primaryHint,
	String modifierHint,
	Operation operation
) {
	enum OperationKind { NONE, PREVIEW, PENDING, CONFLICT, REJECTED }
	record Selection(String name, String type, String dimensions, String sync) { }
	record Operation(OperationKind kind, String title, String detail) {
		static Operation none() { return new Operation(OperationKind.NONE, "", ""); }
	}

	static ViewportOverlayViewModel from(ApertureUiContext context) {
		var selection = context.session.selection().snapshot();
		ObjectEditorView view = selection.primaryObject() == null ? null
			: context.session.readModel().object(selection.primaryObject()).orElse(null);
		Map<String, ParameterValue> previews = view == null ? Map.of() : context.session.preview().values(view.objectId());
		ViewportToolState.Snapshot toolState = ViewportToolState.current();
		return new ViewportOverlayViewModel(context.mode.name(), toolName(context),
			context.snap ? "Snap 10 mm" : "Snap Off", "Local Coordinates",
			Optional.ofNullable(view).map(ViewportOverlayViewModel::selection),
			primaryHint(context), modifierHint(context), operation(view, previews, toolState));
	}

	static Operation operation(ObjectEditorView view, Map<String, ParameterValue> previews,
		ViewportToolState.Snapshot tool) {
		if (view == null) return Operation.none();
		var failure = view.diagnostics().stream().filter(diagnostic -> !diagnostic.resolved())
			.filter(diagnostic -> diagnostic.severity() == EditorDiagnostic.Severity.ERROR).findFirst();
		if (failure.isPresent()) {
			var diagnostic = failure.orElseThrow();
			boolean conflict = diagnostic.code().contains("conflict") || view.syncStatus() == SyncStatus.RESYNC_REQUIRED;
			String subject = diagnostic.path().map(ViewportOverlayViewModel::humanize).orElse("Change");
			return new Operation(conflict ? OperationKind.CONFLICT : OperationKind.REJECTED,
				conflict ? "Revision conflict" : subject + " change rejected",
				conflict ? "Resynchronizing..." : diagnostic.message());
		}
		if (tool.state() == ToolInteractionState.CONFLICT)
			return new Operation(OperationKind.CONFLICT, "Revision conflict", "Resynchronizing...");
		if (tool.state() == ToolInteractionState.REJECTED)
			return new Operation(OperationKind.REJECTED, "Change rejected", "Review the highlighted property");
		if (tool.state() == ToolInteractionState.PENDING || tool.state() == ToolInteractionState.ACCEPTED_WAITING_REPLICA)
			return new Operation(OperationKind.PENDING, "Saving changes...", "Waiting for authoritative replica");
		if (!previews.isEmpty()) {
			var entry = previews.entrySet().iterator().next();
			return new Operation(OperationKind.PREVIEW, humanize(entry.getKey()) + " " + format(entry.getValue()), "Preview");
		}
		return Operation.none();
	}

	private static Selection selection(ObjectEditorView view) {
		return new Selection(view.displayName(), humanize(view.typeId().path()), dimensions(view), switch (view.syncStatus()) {
			case SYNCHRONIZED -> "Synchronized";
			case PREVIEW -> "Preview";
			case RESYNC_REQUIRED -> "Resynchronizing";
		});
	}

	private static String dimensions(ObjectEditorView view) {
		String width = length(view, "width");
		String height = length(view, "height");
		return !width.isBlank() && !height.isBlank() ? width + " x " + height
			: !width.isBlank() ? "Width " + width : !height.isBlank() ? "Height " + height : "";
	}

	private static String length(ObjectEditorView view, String key) {
		return view.parameters().get(key).filter(ParameterValue.LengthValue.class::isInstance)
			.map(ParameterValue.LengthValue.class::cast).map(value -> Math.round(value.millimeters()) + " mm").orElse("");
	}

	private static String toolName(ApertureUiContext context) {
		return humanize(context.session.tools().activeTool().name()) + " Tool";
	}
	private static String primaryHint(ApertureUiContext context) {
		return switch (context.session.tools().activeTool()) {
			case RESIZE -> "Drag a handle to resize";
			case PLACE -> "Aim at a host to place";
			case ROTATE -> "Drag the rotation gizmo";
			default -> "Click an object to select";
		};
	}
	private static String modifierHint(ApertureUiContext context) {
		return context.session.tools().activeTool() == dev.aperture.editor.model.session.ToolController.Tool.RESIZE
			? "Shift: fine adjustment   Ctrl: disable snap   Esc: cancel" : "Esc: cancel";
	}
	private static String format(ParameterValue value) {
		return value instanceof ParameterValue.LengthValue length ? Math.round(length.millimeters()) + " mm"
			: value instanceof ParameterValue.AngleValue angle ? Math.round(angle.degrees()) + " deg"
			: value instanceof ParameterValue.CountValue count ? Integer.toString(count.value())
			: value instanceof ParameterValue.NumberValue number ? Double.toString(number.value())
			: value instanceof ParameterValue.BoolValue bool ? Boolean.toString(bool.value()) : value.asString();
	}
	private static String humanize(String value) {
		String text = value.replace('_', ' ').replace('.', ' ');
		return text.isBlank() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}
