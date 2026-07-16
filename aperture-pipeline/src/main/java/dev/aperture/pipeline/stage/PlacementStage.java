package dev.aperture.pipeline.stage;

import dev.aperture.geometry.collision.CollisionShape;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Placement calculation stage.
 * <p>
 * Calculates placement information from the collision shape, including attachment
 * points, orientation hints, bounding dimensions, and snap points for world placement.
 * This information is used by the renderer to correctly position and orient openings
 * in the Minecraft world.
 * <p>
 * Input: {@link CollisionShape} (collision geometry)
 * Output: {@link PlacementInfo} (placement metadata)
 */
public final class PlacementStage implements PipelineStage<CollisionShape, PlacementStage.PlacementInfo> {

	@Override
	public String name() {
		return "placement";
	}

	@Override
	public StageResult<PlacementInfo> execute(CollisionShape input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Calculating placement information");

		try {
			// Calculate bounding box
			BoundingBox bounds = input.getBoundingBox();
			ctx.debug("Bounds: " + bounds);

			// Calculate attachment point (center of bottom face)
			Vec3 attachmentPoint = new Vec3(
				bounds.center().x(),
				bounds.min().y(),
				bounds.center().z()
			);

			// Calculate dimensions
			Vec3 dimensions = new Vec3(
				bounds.max().x() - bounds.min().x(),
				bounds.max().y() - bounds.min().y(),
				bounds.max().z() - bounds.min().z()
			);

			// Determine primary axis (longest dimension)
			Axis primaryAxis = determinePrimaryAxis(dimensions);
			ctx.debug("Primary axis: " + primaryAxis);

			// Calculate snap points (corners of bounding box at ground level)
			Vec3[] snapPoints = new Vec3[] {
				new Vec3(bounds.min().x(), bounds.min().y(), bounds.min().z()),
				new Vec3(bounds.max().x(), bounds.min().y(), bounds.min().z()),
				new Vec3(bounds.max().x(), bounds.min().y(), bounds.max().z()),
				new Vec3(bounds.min().x(), bounds.min().y(), bounds.max().z())
			};

			PlacementInfo info = new PlacementInfo(
				attachmentPoint,
				dimensions,
				bounds,
				primaryAxis,
				snapPoints
			);

			ctx.debug("Placement info calculated successfully");

			return new StageResult.Success<>(info);

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to calculate placement info: " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Determine which axis is longest (primary orientation).
	 */
	private Axis determinePrimaryAxis(Vec3 dimensions) {
		if (dimensions.x() >= dimensions.y() && dimensions.x() >= dimensions.z()) {
			return Axis.X;
		} else if (dimensions.y() >= dimensions.x() && dimensions.y() >= dimensions.z()) {
			return Axis.Y;
		} else {
			return Axis.Z;
		}
	}

	/**
	 * Placement metadata for opening instances.
	 */
	public record PlacementInfo(
		Vec3 attachmentPoint,
		Vec3 dimensions,
		BoundingBox bounds,
		Axis primaryAxis,
		Vec3[] snapPoints
	) {
		public PlacementInfo {
			Objects.requireNonNull(attachmentPoint, "attachmentPoint cannot be null");
			Objects.requireNonNull(dimensions, "dimensions cannot be null");
			Objects.requireNonNull(bounds, "bounds cannot be null");
			Objects.requireNonNull(primaryAxis, "primaryAxis cannot be null");
			Objects.requireNonNull(snapPoints, "snapPoints cannot be null");
			snapPoints = snapPoints.clone(); // Defensive copy
		}

		/**
		 * Get the volume of the opening's bounding box.
		 */
		public double volume() {
			return dimensions.x() * dimensions.y() * dimensions.z();
		}

		/**
		 * Check if opening fits within given constraints.
		 */
		public boolean fitsWithin(Vec3 maxDimensions) {
			return dimensions.x() <= maxDimensions.x()
				&& dimensions.y() <= maxDimensions.y()
				&& dimensions.z() <= maxDimensions.z();
		}
	}

	/**
	 * Primary orientation axis.
	 */
	public enum Axis {
		X, Y, Z
	}
}
