package dev.aperture.core.parameter;

/**
 * Supported parameter types in the opening parameter engine.
 * Internal values use logical units (millimeters for length, degrees for angle).
 */
public enum ParameterType {
	LENGTH,
	ANGLE,
	COUNT,
	ENUM,
	BOOL,
	MATERIAL_REF
}
