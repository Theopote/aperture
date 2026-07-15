package dev.aperture.core.editor.manipulation;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.parametric.ParametricSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds standard manipulators and resize handles from an opening type's parametric schema.
 */
public final class EditorManipulatorFactory {
	private EditorManipulatorFactory() {
	}

	public static ManipulationLayout layout(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet resolved = definition.parametricSchema().mergeDefaults(instance.parameters());
		double width = length(resolved, "width").orElse(1000);
		double height = length(resolved, "height").orElse(1500);
		double depth = thickness(resolved).orElse(80);

		List<ResizeHandle> handles = new ArrayList<>();
		Map<String, ResizeHandle> handlesById = new LinkedHashMap<>();
		resolveParameter(definition.parametricSchema(), ResizeAxis.WIDTH).ifPresent(parameter -> {
			ResizeHandle handle = new ResizeHandle(
				"width.positive",
				ResizeAxis.WIDTH,
				parameter,
				new Vec3d(width, height / 2.0, depth / 2.0),
				new Vec3d(1, 0, 0)
			);
			handles.add(handle);
			handlesById.put(handle.id(), handle);
		});
		resolveParameter(definition.parametricSchema(), ResizeAxis.HEIGHT).ifPresent(parameter -> {
			ResizeHandle handle = new ResizeHandle(
				"height.positive",
				ResizeAxis.HEIGHT,
				parameter,
				new Vec3d(width / 2.0, height, depth / 2.0),
				new Vec3d(0, 1, 0)
			);
			handles.add(handle);
			handlesById.put(handle.id(), handle);
		});
		resolveParameter(definition.parametricSchema(), ResizeAxis.THICKNESS).ifPresent(parameter -> {
			ResizeHandle handle = new ResizeHandle(
				"thickness.positive",
				ResizeAxis.THICKNESS,
				parameter,
				new Vec3d(width / 2.0, height / 2.0, depth),
				new Vec3d(0, 0, 1)
			);
			handles.add(handle);
			handlesById.put(handle.id(), handle);
		});

		Vec3d center = new Vec3d(width / 2.0, height / 2.0, depth / 2.0);
		List<Manipulator> manipulators = new ArrayList<>();
		manipulators.add(Manipulator.translate(center));
		manipulators.add(Manipulator.rotate(center));
		manipulators.add(Manipulator.mirror(MirrorAxis.X, center));
		for (ResizeHandle handle : handles) {
			manipulators.add(Manipulator.resize(handle));
		}

		return new ManipulationLayout(handles, handlesById, manipulators);
	}

	private static Optional<String> resolveParameter(ParametricSchema schema, ResizeAxis axis) {
		for (String name : axis.parameterNames()) {
			if (schema.get(name).isPresent()) {
				return Optional.of(name);
			}
		}
		return Optional.empty();
	}

	private static Optional<Double> length(ParameterSet parameters, String name) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}

	private static Optional<Double> thickness(ParameterSet parameters) {
		for (String name : ResizeAxis.THICKNESS.parameterNames()) {
			Optional<Double> value = length(parameters, name);
			if (value.isPresent()) {
				return value;
			}
		}
		return Optional.empty();
	}

	public record ManipulationLayout(
		List<ResizeHandle> resizeHandles,
		Map<String, ResizeHandle> resizeHandlesById,
		List<Manipulator> manipulators
	) {
	}
}
