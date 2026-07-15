package dev.aperture.geometry.pipeline;

import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.geometry.pipeline.accessory.AccessoryGenerator;
import dev.aperture.geometry.pipeline.decoration.DecorationGenerator;
import dev.aperture.geometry.pipeline.frame.FrameGenerator;
import dev.aperture.geometry.pipeline.glass.GlassGenerator;
import dev.aperture.geometry.pipeline.hardware.HardwareGenerator;
import dev.aperture.geometry.pipeline.header.HeaderGenerator;
import dev.aperture.geometry.pipeline.mesh.MeshAssembler;
import dev.aperture.geometry.pipeline.panel.PanelGenerator;
import dev.aperture.geometry.pipeline.profile.ProfileGenerator;
import dev.aperture.geometry.pipeline.sill.SillGenerator;
import dev.aperture.geometry.pipeline.trim.TrimGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds a generator pipeline from an opening's component assembly.
 * Window, door, and curtain wall differ only by which steps are included.
 */
public final class ComponentPipelinePlanner {
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

	private ComponentPipelinePlanner() {
	}

	public static OpeningPipeline pipelineFor(ComponentAssembly assembly) {
		List<PipelineStep> steps = new ArrayList<>();
		steps.add(new ProfileGenerator());
		for (ComponentKind kind : STEP_ORDER) {
			if (assembly.has(kind)) {
				steps.add(STEP_FACTORIES.get(kind).get());
			}
		}
		if (!assembly.has(ComponentKind.DIVIDER)) {
			steps.add(new AccessoryGenerator());
		}
		return new OpeningPipeline(steps, new MeshAssembler());
	}

	public static List<String> plannedStepIds(ComponentAssembly assembly) {
		return pipelineFor(assembly).stepIds();
	}
}
