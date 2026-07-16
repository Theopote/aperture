package dev.aperture.kernel;

import dev.aperture.pipeline.stage.PlacementStage;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of opening generation.
 * <p>
 * Sealed interface with Success and Failure variants.
 */
public sealed interface OpeningResult {

	/**
	 * Successful generation result.
	 */
	record Success(
		String typeId,
		PlacementStage.PlacementInfo placement,
		GenerationMetrics metrics
	) implements OpeningResult {
		public Success {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(placement, "placement cannot be null");
			Objects.requireNonNull(metrics, "metrics cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public String typeId() {
			return typeId;
		}

		/**
		 * Get the final placement information.
		 */
		public PlacementStage.PlacementInfo getPlacement() {
			return placement;
		}

		/**
		 * Get generation metrics.
		 */
		public GenerationMetrics getMetrics() {
			return metrics;
		}
	}

	/**
	 * Failed generation result.
	 */
	record Failure(
		String typeId,
		String failedStage,
		String errorMessage,
		Throwable cause
	) implements OpeningResult {
		public Failure {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(failedStage, "failedStage cannot be null");
			Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
			// cause can be null
		}

		/**
		 * Convenience constructor without cause.
		 */
		public Failure(String typeId, String failedStage, String errorMessage) {
			this(typeId, failedStage, errorMessage, null);
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		@Override
		public String typeId() {
			return typeId;
		}

		/**
		 * Get optional cause.
		 */
		public Optional<Throwable> getCause() {
			return Optional.ofNullable(cause);
		}
	}

	/**
	 * Check if generation was successful.
	 */
	boolean isSuccess();

	/**
	 * Get the opening type ID.
	 */
	String typeId();

	/**
	 * Get as Success or throw.
	 */
	default Success asSuccess() {
		if (this instanceof Success s) {
			return s;
		}
		throw new IllegalStateException("Result is not successful");
	}

	/**
	 * Get as Failure or throw.
	 */
	default Failure asFailure() {
		if (this instanceof Failure f) {
			return f;
		}
		throw new IllegalStateException("Result is not a failure");
	}
}
