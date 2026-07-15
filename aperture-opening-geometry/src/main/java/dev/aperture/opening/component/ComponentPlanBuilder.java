package dev.aperture.opening.component;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.DecorationComponent;
import dev.aperture.core.component.DividerComponent;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.GlassComponent;
import dev.aperture.core.component.HardwareComponent;
import dev.aperture.core.component.HeaderComponent;
import dev.aperture.core.component.OpeningComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.component.SillComponent;
import dev.aperture.core.component.TrimComponent;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
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
import java.util.Comparator;
import java.util.List;

/**
 * Component builder layer: one geometry step per component instance.
 */
public final class ComponentPlanBuilder {
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
		List<OpeningComponent> ordered = orderComponents(assembly.all());
		for (OpeningComponent component : ordered) {
			steps.add(createStep(component));
		}
		if (!assembly.has(ComponentKind.DIVIDER)) {
			steps.add(new AccessoryGenerator(DividerComponent.mullions("_mullions")));
		}
		if (steps.isEmpty()) {
			throw new IllegalArgumentException("component assembly produced no geometry steps");
		}
		return new ComponentPlan(steps);
	}

	public static List<String> plannedStepIds(ComponentAssembly assembly) {
		return buildPlan(assembly).stepIds();
	}

	private static List<OpeningComponent> orderComponents(List<OpeningComponent> components) {
		List<OpeningComponent> ordered = new ArrayList<>(components);
		ordered.sort(Comparator.comparingInt(component -> kindOrder(component.kind())));
		return ordered;
	}

	private static int kindOrder(ComponentKind kind) {
		int index = STEP_ORDER.indexOf(kind);
		return index >= 0 ? index : STEP_ORDER.size();
	}

	private static PipelineStep createStep(OpeningComponent component) {
		return switch (component.kind()) {
			case FRAME -> new FrameGenerator((FrameComponent) component);
			case HEADER -> new HeaderGenerator((HeaderComponent) component);
			case PANEL -> new PanelGenerator((PanelComponent) component);
			case GLASS -> new GlassGenerator((GlassComponent) component);
			case DIVIDER -> new AccessoryGenerator((DividerComponent) component);
			case SILL -> new SillGenerator((SillComponent) component);
			case TRIM -> new TrimGenerator((TrimComponent) component);
			case HARDWARE -> new HardwareGenerator((HardwareComponent) component);
			case DECORATION -> new DecorationGenerator((DecorationComponent) component);
		};
	}
}
