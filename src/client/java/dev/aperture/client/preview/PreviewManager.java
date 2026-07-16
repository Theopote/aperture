package dev.aperture.client.preview;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.pipeline.PipelineResultCache;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import dev.aperture.opening.geometry.generator.RectangularWindowGenerator;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Manages preview generation for the parameter editor.
 * Handles async generation, caching, and update notifications.
 */
public class PreviewManager {

    private static final Executor GENERATION_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Aperture-Preview-Generator");
        t.setDaemon(true);
        return t;
    });

    private final PipelineResultCache cache = new PipelineResultCache(10);
    private final RectangularWindowGenerator generator = new RectangularWindowGenerator();

    private @Nullable CompletableFuture<PipelineResult> currentGeneration;
    private @Nullable PreviewUpdateListener updateListener;

    /**
     * Sets the listener to be notified when preview updates.
     */
    public void setUpdateListener(@Nullable PreviewUpdateListener listener) {
        this.updateListener = listener;
    }

    /**
     * Requests a preview generation for the given parameters.
     * Cancels any in-flight generation and starts a new one.
     *
     * @param definition Opening type definition
     * @param parameters Current parameter values
     */
    public void requestPreview(OpeningTypeDefinition definition, ParameterSet parameters) {
        // Cancel any in-flight generation
        if (currentGeneration != null && !currentGeneration.isDone()) {
            currentGeneration.cancel(false);
        }

        // Start new generation
        currentGeneration = CompletableFuture.supplyAsync(() -> {
            return generatePreview(definition, parameters);
        }, GENERATION_EXECUTOR).thenApply(result -> {
            // Notify listener on completion
            if (updateListener != null) {
                updateListener.onPreviewUpdated(result);
            }
            return result;
        }).exceptionally(throwable -> {
            // Handle errors
            if (updateListener != null) {
                updateListener.onPreviewError(throwable);
            }
            return null;
        });
    }

    /**
     * Generates preview using cache.
     */
    private PipelineResult generatePreview(OpeningTypeDefinition definition, ParameterSet parameters) {
        return cache.getOrCompute(
            definition.id().toString(),
            parameters,
            () -> generateUncached(definition, parameters)
        );
    }

    /**
     * Generates preview without cache (actual generation).
     */
    private PipelineResult generateUncached(OpeningTypeDefinition definition, ParameterSet parameters) {
        GenerationContext context = new GenerationContext(
            definition,
            definition.resolveParameters(parameters),
            GenerationTestSupport.profiles()
        );

        return generator.generate(context);
    }

    /**
     * Invalidates cache for the given opening type.
     */
    public void invalidateCache(String typeId) {
        cache.invalidateType(typeId);
    }

    /**
     * Clears all cached previews.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Gets cache statistics for debugging.
     */
    public PipelineResultCache.CacheStats getCacheStats() {
        return cache.stats();
    }

    /**
     * Listener for preview updates.
     */
    public interface PreviewUpdateListener {
        /**
         * Called when preview generation completes successfully.
         */
        void onPreviewUpdated(PipelineResult result);

        /**
         * Called when preview generation fails.
         */
        void onPreviewError(Throwable error);
    }
}
