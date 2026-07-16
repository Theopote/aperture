package dev.aperture.kernel;

import dev.aperture.pipeline.stage.BasicPlacementMetadataStage;
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
		dev.aperture.geometry.pipeline.PipelineResult output,
		BasicPlacementMetadataStage.PlacementInfo placement,
		GenerationMetrics metrics
	) implements OpeningResult {
		public Success {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(output, "output cannot be null");
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
		public BasicPlacementMetadataStage.PlacementInfo getPlacement() {
			return placement;
		}

		/**
		 * Get generation metrics.
		 */
		public GenerationMetrics getMetrics() {
			return metrics;
		}

		@Override
		public boolean isFailure() {
			return false;
		}
	}

	/**
	 * Failed generation result.
	 */
	record Failure(String typeId, KernelDiagnostic diagnostic) implements OpeningResult {
		public Failure { Objects.requireNonNull(typeId, "typeId"); Objects.requireNonNull(diagnostic, "diagnostic"); }
		@Override public boolean isSuccess() { return false; }
		@Override public String typeId() { return typeId; }
		public String failedStage() { return diagnostic.stage().externalName(); }
		public String errorMessage() { return diagnostic.message(); }
		public Throwable cause() { return diagnostic.cause(); }
		public String stage() { return failedStage(); }
		public String message() { return diagnostic.message(); }
		public Optional<Throwable> getCause() { return Optional.ofNullable(diagnostic.cause()); }
		@Override public boolean isFailure() { return true; }
	}
	/**
	 * Check if generation was successful.
	 */
	boolean isSuccess();

	/**
	 * Check if generation failed.
	 */
	default boolean isFailure() {
		return !isSuccess();
	}

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
