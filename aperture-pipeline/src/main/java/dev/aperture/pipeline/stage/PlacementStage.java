package dev.aperture.pipeline.stage;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Arrays;
import java.util.Objects;

/** Calculates platform-neutral placement metadata from collision bounds. */
public final class PlacementStage implements PipelineStage<BoundingBox, PlacementStage.PlacementInfo> {
	@Override
	public String name() {
		return "placement";
	}

	@Override
	public dev.aperture.pipeline.StageId id() { return dev.aperture.pipeline.StageId.PLACEMENT; }

	@Override
	public Class<?> inputType() { return BoundingBox.class; }

	@Override
	public Class<?> outputType() { return PlacementInfo.class; }

	@Override
	public StageResult<PlacementInfo> execute(BoundingBox bounds, StageContext ctx) {
		Objects.requireNonNull(bounds, "input cannot be null");
		Vec3d dimensions = new Vec3d(bounds.width(), bounds.height(), bounds.depth());
		Vec3d attachmentPoint = new Vec3d(
			(bounds.min().x() + bounds.max().x()) / 2.0,
			bounds.min().y(),
			(bounds.min().z() + bounds.max().z()) / 2.0
		);
		Vec3d[] snapPoints = {
			new Vec3d(bounds.min().x(), bounds.min().y(), bounds.min().z()),
			new Vec3d(bounds.max().x(), bounds.min().y(), bounds.min().z()),
			new Vec3d(bounds.max().x(), bounds.min().y(), bounds.max().z()),
			new Vec3d(bounds.min().x(), bounds.min().y(), bounds.max().z())
		};
		return new StageResult.Success<>(new PlacementInfo(
			attachmentPoint, dimensions, bounds, determinePrimaryAxis(dimensions), snapPoints
		));
	}

	private static Axis determinePrimaryAxis(Vec3d dimensions) {
		if (dimensions.x() >= dimensions.y() && dimensions.x() >= dimensions.z()) {
			return Axis.X;
		}
		return dimensions.y() >= dimensions.z() ? Axis.Y : Axis.Z;
	}

	public record PlacementInfo(
		Vec3d attachmentPoint,
		Vec3d dimensions,
		BoundingBox bounds,
		Axis primaryAxis,
		Vec3d[] snapPoints
	) {
		public PlacementInfo {
			Objects.requireNonNull(attachmentPoint, "attachmentPoint cannot be null");
			Objects.requireNonNull(dimensions, "dimensions cannot be null");
			Objects.requireNonNull(bounds, "bounds cannot be null");
			Objects.requireNonNull(primaryAxis, "primaryAxis cannot be null");
			Objects.requireNonNull(snapPoints, "snapPoints cannot be null");
			snapPoints = Arrays.copyOf(snapPoints, snapPoints.length);
		}

		@Override
		public Vec3d[] snapPoints() {
			return Arrays.copyOf(snapPoints, snapPoints.length);
		}

		public double volume() {
			return dimensions.x() * dimensions.y() * dimensions.z();
		}

		public boolean fitsWithin(Vec3d maxDimensions) {
			return dimensions.x() <= maxDimensions.x()
				&& dimensions.y() <= maxDimensions.y()
				&& dimensions.z() <= maxDimensions.z();
		}
	}

	public enum Axis { X, Y, Z }
}
