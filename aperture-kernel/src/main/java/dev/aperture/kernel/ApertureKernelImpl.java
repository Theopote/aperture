package dev.aperture.kernel;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.registry.OpeningTypeRegistry;
import dev.aperture.kernel.internal.KernelConfig;
import dev.aperture.kernel.internal.ResultMapper;
import dev.aperture.kernel.internal.StatsCollector;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ApertureKernel}.
 * <p>
 * Package-private - use {@link ApertureKernel#builder()} to create instances.
 */
final class ApertureKernelImpl implements ApertureKernel {

	private final OpeningTypeRegistry registry;
	private final OpeningPipelineAdapter pipeline;
	private final StatsCollector statsCollector;
	private final ExecutorService executorService;
	private final boolean enableDebugLogging;
	private volatile boolean closed = false;

	ApertureKernelImpl(KernelConfig config) {
		this.registry = config.registry();
		this.pipeline = config.pipeline();
		this.statsCollector = new StatsCollector();
		this.executorService = config.executorService();
		this.enableDebugLogging = config.enableDebugLogging();
	}

	@Override
	public OpeningResult generate(OpeningRequest request) {
		Objects.requireNonNull(request, "request cannot be null");
		ensureNotClosed();

		request.validate();

		long startTime = System.currentTimeMillis();

		try {
			if (enableDebugLogging) {
				log("Generating opening: " + request.typeId());
			}

			// Execute pipeline
			PipelineResult pipelineResult = pipeline.execute(
				request.typeId(),
				request.parameters()
			);

			// Map result
			OpeningResult result = ResultMapper.map(request, pipelineResult);

			// Collect statistics
			long duration = System.currentTimeMillis() - startTime;
			statsCollector.record(result, duration);

			// Update cache stats
			statsCollector.updateCacheStats(pipeline.getCacheStats());

			if (enableDebugLogging) {
				log("Generation completed: " +
					(result.isSuccess() ? "SUCCESS" : "FAILURE") +
					" in " + duration + "ms");
			}

			return result;

		} catch (Exception e) {
			// Unexpected exception - not from pipeline
			long duration = System.currentTimeMillis() - startTime;
			statsCollector.recordFailure(request.typeId(), "exception");

			if (enableDebugLogging) {
				log("Generation failed with exception: " + e.getMessage());
			}

			return new OpeningResult.Failure(
				request.typeId(),
				"exception",
				"Unexpected error: " + e.getMessage(),
				e
			);
		}
	}

	@Override
	public OpeningResult generate(String typeId, Map<String, Object> parameters) {
		Objects.requireNonNull(typeId, "typeId cannot be null");
		Objects.requireNonNull(parameters, "parameters cannot be null");

		return generate(new OpeningRequest(typeId, parameters));
	}

	@Override
	public List<OpeningResult> generateBatch(List<OpeningRequest> requests) {
		Objects.requireNonNull(requests, "requests cannot be null");
		ensureNotClosed();

		if (requests.isEmpty()) {
			return List.of();
		}

		if (enableDebugLogging) {
			log("Generating batch of " + requests.size() + " openings");
		}

		long batchStartTime = System.currentTimeMillis();

		// Sequential execution to maximize cache benefits
		// Requests with same typeId and similar parameters benefit most
		List<OpeningResult> results = requests.stream()
			.map(this::generate)
			.toList();

		long batchDuration = System.currentTimeMillis() - batchStartTime;

		if (enableDebugLogging) {
			long successCount = results.stream()
				.filter(OpeningResult::isSuccess)
				.count();

			double avgTime = (double) batchDuration / requests.size();

			log("Batch completed: " + successCount + "/" + requests.size() +
				" successful, avg " + String.format("%.1fms", avgTime) + " per opening");
		}

		return results;
	}

	/**
	 * Generate multiple openings concurrently.
	 * <p>
	 * Unlike {@link #generateBatch}, this executes requests in parallel
	 * using the kernel's thread pool. Best for independent requests where
	 * cache sharing is less important than throughput.
	 * <p>
	 * Package-private for now - can be made public if needed.
	 */
	List<OpeningResult> generateBatchAsync(List<OpeningRequest> requests) {
		Objects.requireNonNull(requests, "requests cannot be null");
		ensureNotClosed();

		if (requests.isEmpty()) {
			return List.of();
		}

		if (enableDebugLogging) {
			log("Generating batch asynchronously: " + requests.size() + " openings");
		}

		// Execute all in parallel
		List<CompletableFuture<OpeningResult>> futures = requests.stream()
			.map(this::generateAsync)
			.toList();

		// Wait for all to complete
		CompletableFuture<Void> allOf = CompletableFuture.allOf(
			futures.toArray(new CompletableFuture[0])
		);

		// Collect results
		return allOf.thenApply(v ->
			futures.stream()
				.map(CompletableFuture::join)
				.toList()
		).join();
	}

	@Override
	public CompletableFuture<OpeningResult> generateAsync(OpeningRequest request) {
		Objects.requireNonNull(request, "request cannot be null");
		ensureNotClosed();

		return CompletableFuture.supplyAsync(
			() -> generate(request),
			executorService
		);
	}

	@Override
	public <T> PartialResult<T> generateUntil(OpeningRequest request, String stageName) {
		Objects.requireNonNull(request, "request cannot be null");
		Objects.requireNonNull(stageName, "stageName cannot be null");
		ensureNotClosed();

		request.validate();

		// Validate stage name
		String[] validStages = {
			"definition", "parameter", "constraint", "component",
			"geometry", "mesh", "collision", "placement"
		};

		boolean validStage = false;
		for (String valid : validStages) {
			if (valid.equals(stageName)) {
				validStage = true;
				break;
			}
		}

		if (!validStage) {
			throw new IllegalArgumentException(
				"Unknown stage: " + stageName + ". Valid stages: " +
				String.join(", ", validStages)
			);
		}

		long startTime = System.currentTimeMillis();

		try {
			if (enableDebugLogging) {
				log("Generating until stage: " + stageName + " for " + request.typeId());
			}

			// Execute full pipeline (we don't have partial execution yet)
			// This is a limitation of current Pipeline API
			// TODO: Enhance Pipeline to support partial execution
			PipelineResult pipelineResult = pipeline.execute(
				request.typeId(),
				request.parameters()
			);

			long duration = System.currentTimeMillis() - startTime;

			if (pipelineResult.isSuccess()) {
				// Extract the requested stage output
				// Note: This assumes we can get intermediate results
				// For now, we'll return the final output if stage is "placement"

				if ("placement".equals(stageName)) {
					Object finalOutput = pipelineResult.getFinalOutput();
					GenerationMetrics metrics = ResultMapper.buildMetrics(pipelineResult);

					@SuppressWarnings("unchecked")
					T value = (T) finalOutput;

					return new PartialResult.Success<>(
						request.typeId(),
						stageName,
						value,
						pipelineResult.stageCount(),
						metrics
					);
				} else {
					// For other stages, we need Pipeline API enhancement
					return new PartialResult.Failure<>(
						request.typeId(),
						stageName,
						"Partial execution not fully implemented - only 'placement' stage supported"
					);
				}
			} else {
				return new PartialResult.Failure<>(
					request.typeId(),
					pipelineResult.getFailedStageName(),
					pipelineResult.getFailureMessage(),
					pipelineResult.getFailureCause()
				);
			}

		} catch (Exception e) {
			if (enableDebugLogging) {
				log("Partial generation failed with exception: " + e.getMessage());
			}

			return new PartialResult.Failure<>(
				request.typeId(),
				"exception",
				"Unexpected error: " + e.getMessage(),
				e
			);
		}
	}

	@Override
	public Optional<OpeningTypeDefinition> getDefinition(String typeId) {
		Objects.requireNonNull(typeId, "typeId cannot be null");
		ensureNotClosed();

		return registry.get(typeId);
	}

	@Override
	public Set<String> listTypes() {
		ensureNotClosed();
		return registry.getAllIds();
	}

	@Override
	public void registerType(OpeningTypeDefinition definition) {
		Objects.requireNonNull(definition, "definition cannot be null");
		ensureNotClosed();

		registry.register(definition);

		// Clear cache to avoid stale results
		pipeline.clearCache();

		if (enableDebugLogging) {
			log("Registered type: " + definition.id());
		}
	}

	@Override
	public void clearCache() {
		ensureNotClosed();
		pipeline.clearCache();

		if (enableDebugLogging) {
			log("Cache cleared");
		}
	}

	@Override
	public KernelStats getStats() {
		ensureNotClosed();
		return statsCollector.getStats();
	}

	@Override
	public void resetStats() {
		ensureNotClosed();
		statsCollector.reset();

		if (enableDebugLogging) {
			log("Statistics reset");
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		if (enableDebugLogging) {
			log("Closing kernel...");
		}

		closed = true;

		// Shutdown executor service
		executorService.shutdown();

		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				if (enableDebugLogging) {
					log("Forcing executor shutdown after timeout");
				}
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}

		if (enableDebugLogging) {
			log("Kernel closed");
		}
	}

	private void ensureNotClosed() {
		if (closed) {
			throw new IllegalStateException("Kernel has been closed");
		}
	}

	private void log(String message) {
		System.out.println("[ApertureKernel] " + message);
	}
}
