package dev.aperture.client.parameter;

import dev.aperture.client.preview.PreviewManager;
import dev.aperture.client.preview.PreviewRenderer;
import dev.aperture.editor.ApertureEditor;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.ParametricEditResult;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.core.validation.ValidationResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * In-game parameter editor with live preview.
 */
public final class ParameterEditorScreen extends Screen implements PreviewManager.PreviewUpdateListener {
	private final OpeningTypeDefinition definition;
	private final ParametricEditor editor;
	private final Consumer<ParameterSet> onApply;
	private final PreviewManager previewManager;

	private List<ParameterWidgetFactory.ParameterField> fields = List.of();
	private String statusMessage = "";
	private @Nullable PipelineResult currentPreview;
	private boolean previewDirty = true;

	public ParameterEditorScreen(
		OpeningTypeDefinition definition,
		ParameterSet overrides,
		Consumer<ParameterSet> onApply
	) {
		super(Component.translatable("screen.aperture.parameter_editor"));
		this.definition = definition;
		this.editor = ParametricEditor.fromDefinition(definition, overrides);
		this.onApply = onApply;
		this.previewManager = new PreviewManager();
		this.previewManager.setUpdateListener(this);
	}

	@Override
	protected void init() {
		fields = ParameterWidgetFactory.buildFields(editor, 20, 36, 24);
		for (ParameterWidgetFactory.ParameterField field : fields) {
			if (!field.isGroupHeader()) {
				addRenderableWidget(field.widget());
			}
		}

		int applyY = Math.max(180, 36 + fields.size() * 24 + 12);
		addRenderableWidget(Button.builder(Component.translatable("gui.aperture.apply"), button -> applyChanges())
			.bounds(width / 2 - 154, applyY, 150, 20)
			.build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
			.bounds(width / 2 + 4, applyY, 150, 20)
			.build());

		// Initial preview generation
		requestPreviewUpdate();
	}

	/**
	 * Called when any parameter value changes.
	 */
	private void onParameterChanged() {
		previewDirty = true;
		requestPreviewUpdate();
	}

	/**
	 * Requests preview generation with current parameter values.
	 */
	private void requestPreviewUpdate() {
		// Read current values from widgets
		var currentValues = ParameterWidgetFactory.readValues(fields);

		// Apply to editor (non-destructive)
		ParametricEditResult patchResult = editor.patch(currentValues);
		if (!patchResult.success()) {
			statusMessage = "Invalid parameters";
			return;
		}

		// Request preview generation
		previewManager.requestPreview(definition, editor.overridesOnly());
		statusMessage = "Generating preview...";
	}

	@Override
	public void onPreviewUpdated(PipelineResult result) {
		this.currentPreview = result;
		this.previewDirty = false;
		this.statusMessage = "";

		// Log cache stats for debugging
		var stats = previewManager.getCacheStats();
		if (stats.hits() + stats.misses() > 0) {
			System.out.printf("Preview cache: %s%n", stats);
		}
	}

	@Override
	public void onPreviewError(Throwable error) {
		this.statusMessage = "Preview generation failed: " + error.getMessage();
		error.printStackTrace();
	}

	private void applyChanges() {
		ParametricEditResult patchResult = editor.patch(ParameterWidgetFactory.readValues(fields));
		if (!patchResult.success()) {
			statusMessage = patchResult.issues().getFirst().message();
			return;
		}

		ValidationResult validation = ApertureEditor.get().parametrics().validate(definition, editor.overridesOnly());
		if (!validation.isValid()) {
			statusMessage = validation.issues().getFirst().message();
			return;
		}

		onApply.accept(editor.overridesOnly());
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.text(font, title, width / 2 - font.width(title) / 2, 12, 0xFFFFFFFF, true);

		for (ParameterWidgetFactory.ParameterField field : fields) {
			if (field.isGroupHeader()) {
				graphics.text(font, field.label(), 20, field.y(), 0xFFA0A0A0, false);
			} else {
				graphics.text(font, field.label(), 20, field.y() + 6, 0xFFFFFFFF, false);
			}
		}

		// Render preview if available
		if (currentPreview != null) {
			renderPreview(graphics, partialTick);
		}

		if (!statusMessage.isEmpty()) {
			graphics.text(font, statusMessage, width / 2 - font.width(statusMessage) / 2, height - 40, 0xFFFF5555, false);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	/**
	 * Renders the preview in the right side of the screen.
	 */
	private void renderPreview(GuiGraphicsExtractor graphics, float partialTick) {
		// TODO: update to Minecraft 26.1 render state API
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		super.removed();
		// Clean up preview manager
		previewManager.setUpdateListener(null);
	}
}
