package dev.aperture.opening.component;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.opening.geometry.pipeline.accessory.AccessoryGenerator;
import dev.aperture.opening.geometry.pipeline.decoration.DecorationGenerator;
import dev.aperture.opening.geometry.pipeline.frame.FrameGenerator;
import dev.aperture.opening.geometry.pipeline.glass.GlassGenerator;
import dev.aperture.opening.geometry.pipeline.hardware.HardwareGenerator;
import dev.aperture.opening.geometry.pipeline.header.HeaderGenerator;
import dev.aperture.opening.geometry.pipeline.panel.PanelGenerator;
import dev.aperture.opening.geometry.pipeline.sill.SillGenerator;
import dev.aperture.opening.geometry.pipeline.trim.TrimGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Component builder layer: maps component presence to geometry pipeline steps.
 */
public final class ComponentPlanBuilder {
	private static final Map<ComponentKind, Supplier<PipelineStep>> STEP_FACTORIES = Map.ofEntries(
		Map.entry(ComponentKind.FRAME, FrameGenerator::new),
		Map.entry(ComponentKind.HEADER, HeaderGenerator::new),
		Map.entry(ComponentKind.PANEL, PanelGenerator::new),
		Map.entry(ComponentKind.GLASS, GlassGenerator::new),
		Map.entry(ComponentKind.DIVIDER, AccessoryGenerator::new),
		Map.entry(ComponentKind.SILL, SillGenerator::new),
		Map.entry(ComponentKind.TRIM, TrimGenerator::new),
		Map.entry(ComponentKind.HARDWARE, HardwareGenerator::new),
		Map.entry(ComponentKind.DECORATION, DecorationGenerator::new)
	);

	private static final List<ComponentKind> STEP_ORDER = List.of(
		ComponentKind.FRAME,
		ComponentKind.HEADER,
		ComponentKind.PANEL,
		ComponentKind.GLASS,
		ComponentKind.DIVIDER,
		ComponentKind.SILL,
		ComponentKind.TRIM,
		ComponentKind.HARDWARE,
		ComponentKind.DECORATION
	);

	public ComponentPlan build(ComponentAssembly assembly) {
		return buildPlan(assembly);
	}

	public static ComponentPlan buildPlan(ComponentAssembly assembly) {
		List<PipelineStep> steps = new ArrayList<>();
		for (ComponentKind kind : STEP_ORDER) {
			if (assembly.has(kind)) {
				steps.add(STEP_FACTORIES.get(kind).get());
			}
		}
		if (!assembly.has(ComponentKind.DIVIDER)) {
			steps.add(new AccessoryGenerator());
		}
		if (steps.isEmpty()) {
			throw new IllegalArgumentException("component assembly produced no geometry steps");
		}
		return new ComponentPlan(steps);
	}

	public static List<String> plannedStepIds(ComponentAssembly assembly) {
		return buildPlan(assembly).stepIds();
	}
}
