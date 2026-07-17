package dev.aperture.core.serialization;

import java.util.Objects;

/** Resource and diagnostic environment required for complete deserialization. */
public record DecodeContext(
	DefinitionResolver definitions,
	AssetResolver assets,
	MigrationRegistry migrations,
	DiagnosticSink diagnostics
) {
	public DecodeContext {
		Objects.requireNonNull(definitions, "definitions");
		Objects.requireNonNull(assets, "assets");
		Objects.requireNonNull(migrations, "migrations");
		Objects.requireNonNull(diagnostics, "diagnostics");
	}

	public static DecodeContext of(DefinitionResolver definitions) {
		return new DecodeContext(
			definitions,
			AssetResolver.empty(),
			MigrationRegistry.empty(),
			DiagnosticSink.noop()
		);
	}
}
