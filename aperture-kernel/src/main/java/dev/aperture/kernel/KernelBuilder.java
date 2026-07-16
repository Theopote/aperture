package dev.aperture.kernel;

import dev.aperture.core.registry.OpeningTypeRegistry;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.kernel.internal.KernelConfig;
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builder for creating {@link ApertureKernel} instances.
 * <p>
 * Provides fluent API for configuring the kernel. All dependencies are optional;
 * sensible defaults are used for missing values.
 * <p>
 * Example usage:
 * <pre>{@code
 * ApertureKernel kernel = ApertureKernel.builder()
 *     .withCacheCapacity(1000)
 *     .withAsyncThreadPoolSize(8)
 *     .enableDebugLogging()
 *     .build();
 * }</pre>
 */
public final class KernelBuilder {

	private OpeningTypeRegistry registry;
	private ProfileCatalogRegistry profiles;
	private int cacheCapacity = 100;
	private boolean enableDebugLogging = false;
	private int asyncThreadPoolSize = 4;
	private ExecutorService customExecutorService;

	// Package-private constructor - use ApertureKernel.builder()
	KernelBuilder() {
	}

	/**
	 * Set custom opening type registry.
	 * <p>
	 * Default: {@link OpeningTypeRegistry#getInstance()}
	 */
	public KernelBuilder withRegistry(OpeningTypeRegistry registry) {
		this.registry = registry;
		return this;
	}

	/**
	 * Set custom profile catalog registry.
	 * <p>
	 * Default: {@link ProfileCatalogRegistry#getDefault()}
	 */
	public KernelBuilder withProfiles(ProfileCatalogRegistry profiles) {
		this.profiles = profiles;
		return this;
	}

	/**
	 * Set pipeline cache capacity.
	 * <p>
	 * Higher capacity improves cache hit rate but uses more memory.
	 * Set to 0 to disable caching.
	 * <p>
	 * Default: 100
	 *
	 * @param capacity Maximum number of cached stage results
	 */
	public KernelBuilder withCacheCapacity(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Cache capacity cannot be negative");
		}
		this.cacheCapacity = capacity;
		return this;
	}

	/**
	 * Enable debug logging.
	 * <p>
	 * When enabled, the kernel logs generation start/end and cache operations
	 * to standard output.
	 * <p>
	 * Default: false
	 */
	public KernelBuilder enableDebugLogging() {
		this.enableDebugLogging = true;
		return this;
	}

	/**
	 * Disable debug logging (default).
	 */
	public KernelBuilder disableDebugLogging() {
		this.enableDebugLogging = false;
		return this;
	}

	/**
	 * Set thread pool size for async operations.
	 * <p>
	 * This controls how many openings can be generated concurrently when
	 * using {@link ApertureKernel#generateAsync(OpeningRequest)}.
	 * <p>
	 * Default: 4
	 *
	 * @param size Number of threads (must be > 0)
	 */
	public KernelBuilder withAsyncThreadPoolSize(int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("Thread pool size must be positive");
		}
		this.asyncThreadPoolSize = size;
		return this;
	}

	/**
	 * Set custom executor service for async operations.
	 * <p>
	 * If set, this overrides {@link #withAsyncThreadPoolSize(int)}.
	 * <p>
	 * <b>Warning:</b> The kernel will NOT shut down a custom executor
	 * when closed. You are responsible for managing its lifecycle.
	 *
	 * @param executorService Custom executor service
	 */
	public KernelBuilder withExecutorService(ExecutorService executorService) {
		this.customExecutorService = executorService;
		return this;
	}

	/**
	 * Build the kernel instance.
	 * <p>
	 * This method:
	 * <ol>
	 *   <li>Uses default values for any unset dependencies</li>
	 *   <li>Creates the pipeline adapter with configured cache capacity</li>
	 *   <li>Creates the thread pool if no custom executor was provided</li>
	 *   <li>Constructs and returns the kernel</li>
	 * </ol>
	 *
	 * @return New kernel instance
	 */
	public ApertureKernel build() {
		// Use defaults for missing dependencies
		OpeningTypeRegistry finalRegistry = registry != null
			? registry
			: OpeningTypeRegistry.getInstance();

		ProfileCatalogRegistry finalProfiles = profiles != null
			? profiles
			: ProfileCatalogRegistry.getDefault();

		// Create pipeline with configured cache
		OpeningPipelineAdapter pipeline = cacheCapacity == 0
			? OpeningPipelineAdapter.withoutCache()
			: OpeningPipelineAdapter.withCache(cacheCapacity);

		// Create or use executor service
		ExecutorService finalExecutor = customExecutorService != null
			? customExecutorService
			: Executors.newFixedThreadPool(
				asyncThreadPoolSize,
				runnable -> {
					Thread thread = new Thread(runnable);
					thread.setName("aperture-kernel-async-" + thread.getId());
					thread.setDaemon(true);
					return thread;
				}
			);

		// Build configuration
		KernelConfig config = new KernelConfig(
			finalRegistry,
			pipeline,
			finalProfiles,
			finalExecutor,
			enableDebugLogging
		);

		return new ApertureKernelImpl(config);
	}

	/**
	 * Build kernel with test-friendly defaults.
	 * <p>
	 * Equivalent to:
	 * <pre>{@code
	 * builder()
	 *     .withCacheCapacity(0)          // No cache
	 *     .withAsyncThreadPoolSize(1)    // Single thread
	 *     .enableDebugLogging()          // Debug enabled
	 *     .build()
	 * }</pre>
	 */
	public static ApertureKernel buildForTesting() {
		return new KernelBuilder()
			.withCacheCapacity(0)
			.withAsyncThreadPoolSize(1)
			.enableDebugLogging()
			.build();
	}

	/**
	 * Build kernel with production defaults.
	 * <p>
	 * Equivalent to:
	 * <pre>{@code
	 * builder()
	 *     .withCacheCapacity(1000)       // Large cache
	 *     .withAsyncThreadPoolSize(8)    // More threads
	 *     .disableDebugLogging()         // No debug
	 *     .build()
	 * }</pre>
	 */
	public static ApertureKernel buildForProduction() {
		return new KernelBuilder()
			.withCacheCapacity(1000)
			.withAsyncThreadPoolSize(8)
			.disableDebugLogging()
			.build();
	}
}
