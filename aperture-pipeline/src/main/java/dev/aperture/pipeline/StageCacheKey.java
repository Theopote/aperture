package dev.aperture.pipeline;

import java.util.Objects;

/** Explicit, versioned identity for a cacheable stage computation. */
public record StageCacheKey(
	StageId stage,
	String pipelineVersion,
	String openingTypeId,
	long definitionRevision,
	String parameterFingerprint,
	long assetRevision,
	String compilerVersion,
	String qualityLevel
) {
	public StageCacheKey {
		Objects.requireNonNull(stage, "stage cannot be null");
		Objects.requireNonNull(pipelineVersion, "pipelineVersion cannot be null");
		Objects.requireNonNull(openingTypeId, "openingTypeId cannot be null");
		Objects.requireNonNull(parameterFingerprint, "parameterFingerprint cannot be null");
		Objects.requireNonNull(compilerVersion, "compilerVersion cannot be null");
		Objects.requireNonNull(qualityLevel, "qualityLevel cannot be null");
	}
}