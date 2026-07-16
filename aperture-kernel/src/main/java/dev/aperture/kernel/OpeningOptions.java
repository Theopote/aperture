package dev.aperture.kernel;

import java.util.Objects;

/**
 * Options for opening generation.
 * <p>
 * Controls caching, logging, collision strategy, and mesh quality.
 * Immutable and thread-safe.
 */
public record OpeningOptions(
	boolean enableCache,
	boolean enableDebugLogging,
	CollisionStrategy collisionStrategy,
	MeshQuality meshQuality
) {
	/**
	 * Default options: cache enabled, normal quality, convex hull collision.
	 */
	public static final OpeningOptions DEFAULT = new OpeningOptions(
		true,                               // enableCache
		false,                              // enableDebugLogging
		CollisionStrategy.CONVEX_HULL,      // collisionStrategy
		MeshQuality.NORMAL                  // meshQuality
	);

	/**
	 * Options for testing: cache disabled, debug logging enabled.
	 */
	public static final OpeningOptions TEST = new OpeningOptions(
		false,                              // enableCache
		true,                               // enableDebugLogging
		CollisionStrategy.BOUNDING_BOX,     // collisionStrategy (fastest)
		MeshQuality.LOW                     // meshQuality (fastest)
	);

	/**
	 * Options for production: cache enabled, high quality.
	 */
	public static final OpeningOptions PRODUCTION = new OpeningOptions(
		true,                               // enableCache
		false,                              // enableDebugLogging
		CollisionStrategy.SIMPLIFIED_MESH,  // collisionStrategy (balanced)
		MeshQuality.HIGH                    // meshQuality (best)
	);

	public OpeningOptions {
		Objects.requireNonNull(collisionStrategy, "collisionStrategy cannot be null");
		Objects.requireNonNull(meshQuality, "meshQuality cannot be null");
	}

	/**
	 * Collision shape generation strategy.
	 */
	public enum CollisionStrategy {
		/**
		 * Generate convex hull (fastest collision detection, least accurate).
		 * <p>
		 * Best for: Simple shapes, performance-critical scenarios.
		 */
		CONVEX_HULL,

		/**
		 * Use axis-aligned bounding box (very fast, very approximate).
		 * <p>
		 * Best for: Testing, rough collision checks.
		 */
		BOUNDING_BOX,

		/**
		 * Simplify mesh by reducing triangle count (balanced).
		 * <p>
		 * Best for: Production use, good balance of accuracy and performance.
		 */
		SIMPLIFIED_MESH,

		/**
		 * Use exact mesh (most accurate, slowest).
		 * <p>
		 * Best for: Complex shapes requiring precise collision detection.
		 */
		EXACT_MESH
	}

	/**
	 * Mesh tessellation quality.
	 */
	public enum MeshQuality {
		/**
		 * Low quality: fast generation, fewer triangles, visible faceting.
		 * <p>
		 * Triangle count: ~50-200 per component.
		 * Generation time: ~50-100ms.
		 */
		LOW,

		/**
		 * Normal quality: balanced generation, good visual quality.
		 * <p>
		 * Triangle count: ~200-800 per component.
		 * Generation time: ~100-300ms.
		 */
		NORMAL,

		/**
		 * High quality: slow generation, many triangles, smooth surfaces.
		 * <p>
		 * Triangle count: ~800-3000 per component.
		 * Generation time: ~300-1000ms.
		 */
		HIGH
	}

	// Fluent builder methods

	public OpeningOptions withCache(boolean enabled) {
		return new OpeningOptions(enabled, enableDebugLogging, collisionStrategy, meshQuality);
	}

	public OpeningOptions withDebugLogging(boolean enabled) {
		return new OpeningOptions(enableCache, enabled, collisionStrategy, meshQuality);
	}

	public OpeningOptions withCollisionStrategy(CollisionStrategy strategy) {
		return new OpeningOptions(enableCache, enableDebugLogging, strategy, meshQuality);
	}

	public OpeningOptions withMeshQuality(MeshQuality quality) {
		return new OpeningOptions(enableCache, enableDebugLogging, collisionStrategy, quality);
	}
}
