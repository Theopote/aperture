package dev.aperture.core.parametric;

import dev.aperture.parameter.ParameterType;

/**
 * Logical unit for numeric parameters. Internal storage uses millimeters / degrees / counts / ratios.
 */
public enum NumberUnit {
	LENGTH_MM,
	ANGLE_DEG,
	COUNT,
	RATIO,
	PLAIN;

	public ParameterType storageType() {
		return switch (this) {
			case LENGTH_MM -> ParameterType.LENGTH;
			case ANGLE_DEG -> ParameterType.ANGLE;
			case COUNT -> ParameterType.COUNT;
			case RATIO, PLAIN -> ParameterType.NUMBER;
		};
	}
}
