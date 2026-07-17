package dev.aperture.core.serialization;

import com.google.gson.JsonObject;

/** Migrates one resource kind from schema version N to N+1. */
@FunctionalInterface
public interface MigrationStep {
	JsonObject migrate(JsonObject source);
}
