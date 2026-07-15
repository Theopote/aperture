package dev.aperture.core.placement;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.ValidationResult;

import java.util.Objects;

/**
 * Transient state for an in-progress placement operation.
 */
public record PlacementSession(
	OpeningId selectedTypeId,
	ParameterSet parameterOverrides,
	OpeningInstance previewInstance,
	HostBinding targetHost,
	ValidationResult validationReport
) {
	public PlacementSession {
		Objects.requireNonNull(selectedTypeId, "selectedTypeId");
		Objects.requireNonNull(parameterOverrides, "parameterOverrides");
		Objects.requireNonNull(previewInstance, "previewInstance");
		Objects.requireNonNull(targetHost, "targetHost");
		Objects.requireNonNull(validationReport, "validationReport");
	}

	public boolean isValid() {
		return validationReport.isValid();
	}
}
