package dev.aperture.pipeline.stage;

import dev.aperture.geometry.collision.CollisionShape;
import dev.aperture.geometry.collision.CollisionShapeBuilder;
import dev.aperture.geometry.mesh.TriangleMesh;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Collision shape generation stage.
 * <p>
 * Converts triangle meshes into collision shapes suitable for physics simulation
 * and placement validation. Collision shapes are typically simplified versions
 * of the visual mesh to improve performance.
 * <p>
 * Input: {@link MeshStage.MeshCollection} (triangle meshes)
 * Output: {@link CollisionShape} (simplified collision geometry)
 */
public final class CollisionStage implements PipelineStage<MeshStage.MeshCollection, CollisionShape> {

	private final CollisionShapeBuilder builder;
	private final SimplificationStrategy strategy;

	/**
	 * Create collision stage with default builder and strategy.
	 */
	public CollisionStage() {
		this(new CollisionShapeBuilder(), SimplificationStrategy.CONVEX_HULL);
	}

	/**
	 * Create collision stage with custom builder and strategy.
	 */
	public CollisionStage(CollisionShapeBuilder builder, SimplificationStrategy strategy) {
		this.builder = Objects.requireNonNull(builder, "builder cannot be null");
		this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
	}

	@Override
	public String name() {
		return "collision";
	}

	@Override
	public StageResult<CollisionShape> execute(MeshStage.MeshCollection input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Generating collision shape from " + input.meshes().size() + " meshes");
		ctx.debug("Using strategy: " + strategy);

		try {
			// Merge all layer meshes into single collision shape
			builder.reset();

			for (var entry : input.meshes().entrySet()) {
				String layerName = entry.getKey();
				TriangleMesh mesh = entry.getValue();

				ctx.debug("Adding layer: " + layerName + " (" + mesh.triangleCount() + " triangles)");

				builder.addMesh(mesh);
			}

			// Build collision shape using selected strategy
			CollisionShape shape = switch (strategy) {
				case CONVEX_HULL -> builder.buildConvexHull();
				case BOUNDING_BOX -> builder.buildBoundingBox();
				case SIMPLIFIED_MESH -> builder.buildSimplifiedMesh(0.1); // 10% simplification
				case EXACT_MESH -> builder.buildExactMesh();
			};

			ctx.debug("Collision shape generated: " + shape.vertexCount() + " vertices");

			return new StageResult.Success<>(shape);

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to generate collision shape: " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Strategies for simplifying meshes into collision shapes.
	 */
	public enum SimplificationStrategy {
		/**
		 * Generate convex hull (fastest collision detection, least accurate).
		 */
		CONVEX_HULL,

		/**
		 * Use axis-aligned bounding box (very fast, very approximate).
		 */
		BOUNDING_BOX,

		/**
		 * Simplify mesh by reducing triangle count (balanced).
		 */
		SIMPLIFIED_MESH,

		/**
		 * Use exact mesh (most accurate, slowest).
		 */
		EXACT_MESH
	}
}
