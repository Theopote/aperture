package dev.aperture.core.serialization;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Ordered registry of explicit single-version JSON migrations. */
public final class MigrationRegistry {
	private final Map<Key, MigrationStep> steps;

	private MigrationRegistry(Map<Key, MigrationStep> steps) {
		this.steps = Map.copyOf(steps);
	}

	public static MigrationRegistry empty() {
		return new MigrationRegistry(Map.of());
	}

	public static Builder builder() {
		return new Builder();
	}

	public JsonObject migrate(String resourceKind, JsonObject source, int targetVersion, DiagnosticSink diagnostics) {
		Objects.requireNonNull(resourceKind, "resourceKind");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(diagnostics, "diagnostics");
		if (targetVersion < 1) throw new IllegalArgumentException("targetVersion must be >= 1");

		JsonObject current = source.deepCopy();
		int version = current.has("schemaVersion") ? current.get("schemaVersion").getAsInt() : 1;
		if (version > targetVersion) {
			throw new IllegalArgumentException(
				"Cannot decode future " + resourceKind + " schema " + version + " as " + targetVersion
			);
		}
		while (version < targetVersion) {
			MigrationStep step = steps.get(new Key(resourceKind, version));
			if (step == null) {
				throw new IllegalArgumentException(
					"Missing migration for " + resourceKind + " schema " + version + " -> " + (version + 1)
				);
			}
			current = Objects.requireNonNull(step.migrate(current.deepCopy()), "Migration returned null");
			version++;
			current.addProperty("schemaVersion", version);
			diagnostics.report(new DecodeDiagnostic(
				"decode.migrated",
				DecodeDiagnostic.Severity.INFO,
				"Migrated " + resourceKind + " to schema " + version,
				null
			));
		}
		return current;
	}

	private record Key(String resourceKind, int fromVersion) { }

	public static final class Builder {
		private final Map<Key, MigrationStep> steps = new LinkedHashMap<>();

		public Builder register(String resourceKind, int fromVersion, MigrationStep step) {
			if (resourceKind == null || resourceKind.isBlank()) {
				throw new IllegalArgumentException("resourceKind must not be blank");
			}
			if (fromVersion < 1) throw new IllegalArgumentException("fromVersion must be >= 1");
			Key key = new Key(resourceKind, fromVersion);
			if (steps.putIfAbsent(key, Objects.requireNonNull(step, "step")) != null) {
				throw new IllegalArgumentException("Duplicate migration: " + resourceKind + "@" + fromVersion);
			}
			return this;
		}

		public MigrationRegistry build() {
			return new MigrationRegistry(steps);
		}
	}
}
