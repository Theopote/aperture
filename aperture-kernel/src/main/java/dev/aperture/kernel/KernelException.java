package dev.aperture.kernel;

/**
 * Exception thrown by the Aperture Kernel.
 * <p>
 * Base class for all kernel-specific exceptions.
 */
public class KernelException extends RuntimeException {

	public KernelException(String message) {
		super(message);
	}

	public KernelException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Exception thrown when kernel has been closed.
	 */
	public static class ClosedException extends KernelException {
		public ClosedException() {
			super("Kernel has been closed");
		}
	}

	/**
	 * Exception thrown when a requested stage is unknown.
	 */
	public static class UnknownStageException extends KernelException {
		public UnknownStageException(String stageName) {
			super("Unknown stage: " + stageName);
		}
	}

	/**
	 * Exception thrown when generation fails unexpectedly.
	 */
	public static class GenerationException extends KernelException {
		private final String typeId;

		public GenerationException(String typeId, String message) {
			super("Generation failed for " + typeId + ": " + message);
			this.typeId = typeId;
		}

		public GenerationException(String typeId, String message, Throwable cause) {
			super("Generation failed for " + typeId + ": " + message, cause);
			this.typeId = typeId;
		}

		public String getTypeId() {
			return typeId;
		}
	}

	/**
	 * Exception thrown when a type is not registered.
	 */
	public static class TypeNotFoundException extends KernelException {
		private final String typeId;

		public TypeNotFoundException(String typeId) {
			super("Opening type not found: " + typeId);
			this.typeId = typeId;
		}

		public String getTypeId() {
			return typeId;
		}
	}
}
