package dev.aperture.editor.interaction;

/** Parses architectural length input into logical millimetres. */
public final class DimensionValueParser {
	public ParsedDimension parse(String input, double baseMillimeters) {
		if (input == null || input.isBlank()) throw new IllegalArgumentException("Enter a dimension value");
		String normalized = input.trim().toLowerCase().replace(" ", "");
		boolean relative = normalized.startsWith("+") || normalized.startsWith("-");
		double factor;
		if (normalized.endsWith("mm")) {
			factor = 1.0;
			normalized = normalized.substring(0, normalized.length() - 2);
		} else if (normalized.endsWith("m")) {
			factor = 1000.0;
			normalized = normalized.substring(0, normalized.length() - 1);
		} else factor = 1.0;
		try {
			double entered = Double.parseDouble(normalized) * factor;
			double result = relative ? baseMillimeters + entered : entered;
			if (!Double.isFinite(result) || result < 0) throw new IllegalArgumentException("Dimension must be non-negative");
			return new ParsedDimension(result, relative);
		} catch (NumberFormatException error) {
			throw new IllegalArgumentException("Use a value such as 1350, 1.35m, +100, or -50");
		}
	}

	public record ParsedDimension(double millimeters, boolean relative) { }
}
