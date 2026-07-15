package dev.aperture.geometry.generator.pipeline;

/**
 * Reserved for operable panel geometry (sash, door leaf, sliding panel).
 * Fixed windows have no panel solids in this phase.
 */
public final class PanelStage implements GenerationStage {
	@Override
	public String id() {
		return "panel";
	}

	@Override
	public void contribute(GenerationContext context, GeometryAssemblyBuilder builder) {
		// No panel geometry for fixed windows.
	}
}
