package dev.aperture.geometry.generator.pipeline;

/**
 * One stage in an opening generation pipeline.
 */
public interface GenerationStage {
	String id();

	void contribute(GenerationContext context, GeometryAssemblyBuilder builder);
}
