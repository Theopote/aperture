package dev.aperture.core.definition;

/**
 * A declarative constraint on opening parameters.
 * Expression evaluation is deferred to Phase 3; Phase 0 stores the rule only.
 */
public record ConstraintRule(String expression, String message) {
}
