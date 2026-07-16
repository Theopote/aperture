package dev.aperture.parameter;

/**
 * Supported parameter types in the opening parameter engine.
 * Internal values use logical units (millimeters for length, degrees for angle).
 */
public enum ParameterType {
	LENGTH,
	ANGLE,
	COUNT,
	NUMBER,
	ENUM,
	BOOL,
	MATERIAL_REF
}
