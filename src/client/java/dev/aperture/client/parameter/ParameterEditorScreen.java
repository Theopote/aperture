package dev.aperture.client.parameter;

import dev.aperture.api.ApertureApi;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.ParametricEditResult;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.ValidationResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * In-game parameter editor generated from {@link ParametricEditor#describe(String)}.
 */
public final class ParameterEditorScreen extends Screen {
	private final OpeningTypeDefinition definition;
	private final ParametricEditor editor;
	private final Consumer<ParameterSet> onApply;

	private List<ParameterWidgetFactory.ParameterField> fields = List.of();
	private String statusMessage = "";

	public ParameterEditorScreen(
		OpeningTypeDefinition definition,
		ParameterSet overrides,
		Consumer<ParameterSet> onApply
	) {
		super(Component.translatable("screen.aperture.parameter_editor"));
		this.definition = definition;
		this.editor = ParametricEditor.fromDefinition(definition, overrides);
		this.onApply = onApply;
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
	}

	private void applyChanges() {
		ParametricEditResult patchResult = editor.patch(ParameterWidgetFactory.readValues(fields));
		if (!patchResult.success()) {
			statusMessage = patchResult.issues().getFirst().message();
			return;
		}

		ValidationResult validation = ApertureApi.get().parametrics().validate(definition, editor.overridesOnly());
		if (!validation.isValid()) {
			statusMessage = validation.issues().getFirst().message();
			return;
		}

		onApply.accept(editor.overridesOnly());
		onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

		for (ParameterWidgetFactory.ParameterField field : fields) {
			if (field.isGroupHeader()) {
				graphics.drawString(font, field.label(), 20, field.y(), 0xA0A0A0);
			} else {
				graphics.drawString(font, field.label(), 20, field.y() + 6, 0xFFFFFF);
			}
		}

		if (!statusMessage.isEmpty()) {
			graphics.drawCenteredString(font, statusMessage, width / 2, height - 40, 0xFF5555);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
