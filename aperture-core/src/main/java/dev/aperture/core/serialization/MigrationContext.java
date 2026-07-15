package dev.aperture.core.serialization;

/**
 * Carries version metadata for schema migrations during deserialization.
 * Phase 0 stub — migration pipeline is implemented in a later phase.
 */
public record MigrationContext(int targetSchemaVersion) {
	public static final MigrationContext CURRENT = new MigrationContext(SchemaVersion.OPENING_INSTANCE);

	public MigrationContext {
		if (targetSchemaVersion < 1) {
			throw new IllegalArgumentException("targetSchemaVersion must be >= 1");
		}
	}
}
