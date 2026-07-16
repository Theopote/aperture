package dev.aperture.pipeline.stage;

import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.math.BoundingBox;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Bounding-box-only collision metadata. This is not a convex hull or exact mesh collider. */
public final class BoundingBoxCollisionStage implements PipelineStage<MeshAssembly, BoundingBox> {
	@Override public String name() { return "collision"; }
	@Override public dev.aperture.pipeline.StageId id() { return dev.aperture.pipeline.StageId.COLLISION; }
	@Override public Class<?> inputType() { return MeshAssembly.class; }
	@Override public Class<?> outputType() { return BoundingBox.class; }

	@Override
	public StageResult<BoundingBox> execute(MeshAssembly input, StageContext context) {
		Objects.requireNonNull(input, "input cannot be null");
		return new StageResult.Success<>(input.bounds());
	}
}