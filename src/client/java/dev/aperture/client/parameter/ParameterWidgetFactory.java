package dev.aperture.client.parameter;

import dev.aperture.core.parametric.BooleanParameter;
import dev.aperture.core.parametric.ChoiceOption;
import dev.aperture.core.parametric.ChoiceParameter;
import dev.aperture.core.parametric.EnumParameter;
import dev.aperture.core.parametric.MaterialParameter;
import dev.aperture.core.parametric.NumberParameter;
import dev.aperture.core.parametric.Parameter;
import dev.aperture.core.parametric.ParameterBridge;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.core.parametric.RangeParameter;
import dev.aperture.core.parameter.ParameterValue;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds Minecraft GUI controls from {@link ParametricEditor#describe(String)} metadata.
 */
public final class ParameterWidgetFactory {
	private ParameterWidgetFactory() {
	}

	public static List<ParameterField> buildFields(ParametricEditor editor, int x, int startY, int rowHeight) {
		List<ParameterField> fields = new ArrayList<>();
		int y = startY;
		String currentGroup = null;

		for (String name : editor.parameterNames()) {
			Parameter parameter = editor.describe(name);
			String group = parameter.group();
			if (!group.isEmpty() && !group.equals(currentGroup)) {
				currentGroup = group;
				fields.add(ParameterField.groupHeader(group, y));
				y += rowHeight;
			}

			AbstractWidget widget = switch (parameter) {
				case RangeParameter range -> numericBox(name, range, editor.resolved(name), x + 120, y, 160);
				case NumberParameter number -> numericBox(name, number, editor.resolved(name), x + 120, y, 160);
				case BooleanParameter bool -> toggleButton(name, bool, editor.resolved(name), x + 120, y, 160);
				case ChoiceParameter choice -> cycleChoice(name, choice, editor.resolved(name), x + 120, y, 160);
				case EnumParameter enumParameter -> cycleEnum(name, enumParameter, editor.resolved(name), x + 120, y, 160);
				case MaterialParameter material -> textBox(name, editor.resolved(name), x + 120, y, 160);
			};

			String label = parameter.label().isEmpty() ? name : parameter.label();
			fields.add(new ParameterField(name, parameter, Component.literal(label), widget, y));
			y += rowHeight;
		}

		return fields;
	}

	public static Map<String, Object> readValues(List<ParameterField> fields) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (ParameterField field : fields) {
			if (field.widget() == null) {
				continue;
			}
			values.put(field.name(), field.readValue());
		}
		return values;
	}

	private static EditBox numericBox(String name, Parameter parameter, ParameterValue current, int x, int y, int width) {
		Object external = ParameterBridge.toExternalValue(current);
		EditBox box = new EditBox(
			net.minecraft.client.Minecraft.getInstance().font,
			x,
			y,
			width,
			20,
			Component.literal(name)
		);
		box.setValue(String.valueOf(external));
		return box;
	}

	private static EditBox textBox(String name, ParameterValue current, int x, int y, int width) {
		EditBox box = new EditBox(
			net.minecraft.client.Minecraft.getInstance().font,
			x,
			y,
			width,
			20,
			Component.literal(name)
		);
		box.setValue(((ParameterValue.MaterialRefValue) current).raw());
		return box;
	}

	private static Button toggleButton(String name, BooleanParameter parameter, ParameterValue current, int x, int y, int width) {
		boolean[] value = { ((ParameterValue.BoolValue) current).value() };
		Button[] buttonRef = new Button[1];
		Button button = Button.builder(Component.empty(), ignored -> {
			value[0] = !value[0];
			updateToggleLabel(buttonRef[0], value[0]);
		}).bounds(x, y, width, 20).build();
		buttonRef[0] = button;
		updateToggleLabel(button, value[0]);
		return button;
	}

	private static Button cycleChoice(String name, ChoiceParameter parameter, ParameterValue current, int x, int y, int width) {
		String[] value = { ((ParameterValue.EnumValue) current).value() };
		Button[] buttonRef = new Button[1];
		Button button = Button.builder(Component.empty(), ignored -> {
			int index = indexOfChoice(parameter, value[0]);
			ChoiceOption next = parameter.choices().get((index + 1) % parameter.choices().size());
			value[0] = next.value();
			buttonRef[0].setMessage(choiceLabel(parameter, value[0]));
		}).bounds(x, y, width, 20).build();
		buttonRef[0] = button;
		button.setMessage(choiceLabel(parameter, value[0]));
		return button;
	}

	private static Button cycleEnum(String name, EnumParameter parameter, ParameterValue current, int x, int y, int width) {
		String[] value = { ((ParameterValue.EnumValue) current).value() };
		Button[] buttonRef = new Button[1];
		Button button = Button.builder(Component.empty(), ignored -> {
			int index = parameter.values().indexOf(value[0]);
			value[0] = parameter.values().get((index + 1) % parameter.values().size());
			buttonRef[0].setMessage(Component.literal(value[0]));
		}).bounds(x, y, width, 20).build();
		buttonRef[0] = button;
		button.setMessage(Component.literal(value[0]));
		return button;
	}

	private static Component choiceLabel(ChoiceParameter parameter, String value) {
		for (ChoiceOption option : parameter.choices()) {
			if (option.value().equals(value)) {
				return Component.literal(option.label());
			}
		}
		return Component.literal(value);
	}

	private static int indexOfChoice(ChoiceParameter parameter, String value) {
		for (int i = 0; i < parameter.choices().size(); i++) {
			if (parameter.choices().get(i).value().equals(value)) {
				return i;
			}
		}
		return 0;
	}

	private static String readChoiceValue(Button button, ChoiceParameter parameter) {
		String label = button.getMessage().getString();
		for (ChoiceOption option : parameter.choices()) {
			if (option.label().equals(label)) {
				return option.value();
			}
		}
		return parameter.choices().getFirst().value();
	}

	private static void updateToggleLabel(Button button, boolean value) {
		button.setMessage(Component.literal(value ? "On" : "Off"));
	}

	private static boolean readToggle(Button button) {
		return "On".equals(button.getMessage().getString());
	}

	public record ParameterField(
		String name,
		Parameter parameter,
		Component label,
		AbstractWidget widget,
		int y
	) {
		public static ParameterField groupHeader(String group, int y) {
			return new ParameterField("", null, Component.literal(group), null, y);
		}

		public boolean isGroupHeader() {
			return widget == null;
		}

		public Object readValue() {
			return switch (widget) {
				case EditBox box -> parseBoxValue(box.getValue());
				case Button button -> readButtonValue(button);
				default -> throw new IllegalStateException("Unsupported widget for " + name);
			};
		}

		private Object parseBoxValue(String raw) {
			return switch (parameter) {
				case MaterialParameter ignored -> raw;
				case RangeParameter range when range.unit() == dev.aperture.core.parametric.NumberUnit.COUNT -> Integer.parseInt(raw);
				case NumberParameter number when number.unit() == dev.aperture.core.parametric.NumberUnit.COUNT -> Integer.parseInt(raw);
				default -> Double.parseDouble(raw);
			};
		}

		private Object readButtonValue(Button button) {
			return switch (parameter) {
				case BooleanParameter ignored -> "On".equals(button.getMessage().getString());
				case ChoiceParameter choice -> readChoiceValue(button, choice);
				case EnumParameter ignored2 -> button.getMessage().getString();
				default -> throw new IllegalStateException("Unsupported button parameter " + name);
			};
		}
	}
}
