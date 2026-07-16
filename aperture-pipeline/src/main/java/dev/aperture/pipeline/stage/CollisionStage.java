package dev.aperture.pipeline.stage;

import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.math.BoundingBox;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Derives the platform-neutral collision bounds from the baked mesh assembly. */
public final class CollisionStage implements PipelineStage<MeshAssembly, BoundingBox> {
	private final SimplificationStrategy strategy;

	public CollisionStage() {
		this(SimplificationStrategy.BOUNDING_BOX);
	}

	public CollisionStage(SimplificationStrategy strategy) {
		this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
	}

	@Override
	public String name() {
		return "collision";
	}

	@Override
	public StageResult<BoundingBox> execute(MeshAssembly input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		ctx.debug("Collision strategy " + strategy + " currently resolves to mesh bounds");
		return new StageResult.Success<>(input.bounds());
	}

	public enum SimplificationStrategy {
		CONVEX_HULL,
		BOUNDING_BOX,
		SIMPLIFIED_MESH,
		EXACT_MESH
	}
}
