package dev.aperture.kernel;

import dev.aperture.core.definition.OpeningTypeDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Aperture Kernel - Unified API for opening generation.
 * <p>
 * Provides a high-level, easy-to-use interface that hides the complexity
 * of the underlying pipeline system. This is the primary entry point for
 * opening generation in client applications.
 * <p>
 * <b>Thread Safety:</b> All methods are thread-safe and can be called
 * concurrently from multiple threads.
 * <p>
 * <b>Resource Management:</b> Kernel instances should be closed when no
 * longer needed to release resources (thread pools, caches). Use try-with-resources:
 * <pre>{@code
 * try (ApertureKernel kernel = ApertureKernel.builder().build()) {
 *     OpeningResult result = kernel.generate("aperture:door_standard", params);
 *     // use result...
 * }
 * }</pre>
 *
 * @see OpeningRequest
 * @see OpeningResult
 * @see KernelBuilder
 */
public interface ApertureKernel extends AutoCloseable {

	/**
	 * Create a new kernel builder.
	 */
	static KernelBuilder builder() {
		return new KernelBuilder();
	}

	/**
	 * Generate an opening from a request.
	 * <p>
	 * Executes the complete 8-stage pipeline:
	 * Definition → Parameter → Constraint → Component → Geometry → Mesh → Collision → Placement
	 *
	 * @param request Opening generation request
	 * @return Generation result (success or failure)
	 * @throws NullPointerException if request is null
	 */
	OpeningResult generate(OpeningRequest request);

	/**
	 * Generate an opening (simplified API).
	 * <p>
	 * Equivalent to {@code generate(new OpeningRequest(typeId, parameters))}.
	 *
	 * @param typeId Opening type identifier (e.g., "aperture:door_standard")
	 * @param parameters User parameter overrides
	 * @return Generation result (success or failure)
	 * @throws NullPointerException if typeId or parameters is null
	 */
	OpeningResult generate(String typeId, Map<String, Object> parameters);

	/**
	 * Generate multiple openings in batch.
	 * <p>
	 * Leverages caching to improve performance when generating multiple
	 * openings with similar parameters.
	 *
	 * @param requests List of generation requests
	 * @return List of results (same order as requests)
	 * @throws NullPointerException if requests is null
	 */
	List<OpeningResult> generateBatch(List<OpeningRequest> requests);

	/**
	 * Generate an opening asynchronously.
	 * <p>
	 * Returns immediately with a {@link CompletableFuture} that will complete
	 * when generation finishes. Generation runs on the kernel's thread pool.
	 *
	 * @param request Opening generation request
	 * @return Future that completes with the generation result
	 * @throws NullPointerException if request is null
	 */
	CompletableFuture<OpeningResult> generateAsync(OpeningRequest request);

	/**
	 * Get opening type definition.
	 *
	 * @param typeId Type identifier (e.g., "aperture:door_standard")
	 * @return Definition if registered, empty otherwise
	 */
	Optional<OpeningTypeDefinition> getDefinition(String typeId);

	/**
	 * List all registered opening type IDs.
	 *
	 * @return Set of type identifiers
	 */
	Set<String> listTypes();

	/**
	 * Register a new opening type.
	 * <p>
	 * <b>Warning:</b> This clears the pipeline cache to prevent using
	 * stale cached results.
	 *
	 * @param definition Type definition to register
	 * @throws NullPointerException if definition is null
	 */
	void registerType(OpeningTypeDefinition definition);

	/**
	 * Clear the pipeline cache.
	 * <p>
	 * Call this when opening definitions have changed or when you want
	 * to ensure fresh computation (e.g., for testing).
	 */
	void clearCache();

	/**
	 * Get kernel statistics.
	 * <p>
	 * Returns aggregate statistics across all generation requests since
	 * the kernel was created or since {@link #resetStats()} was called.
	 *
	 * @return Current statistics
	 */
	KernelStats getStats();

	/**
	 * Reset statistics counters.
	 * <p>
	 * Resets request counts, timing averages, and failure counts to zero.
	 * Does not affect the cache.
	 */
	void resetStats();

	/**
	 * Close the kernel and release resources.
	 * <p>
	 * Shuts down the thread pool and clears caches. The kernel cannot
	 * be used after closing.
	 * <p>
	 * This method waits up to 5 seconds for pending async operations
	 * to complete before forcing shutdown.
	 */
	@Override
	void close();
}
