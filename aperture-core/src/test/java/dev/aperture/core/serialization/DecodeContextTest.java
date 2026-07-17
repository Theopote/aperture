package dev.aperture.core.serialization;

import com.google.gson.JsonObject;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecodeContextTest {
	@Test
	void genericCodecEntryPointResolvesDefinitionAndCompletesDecode() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		OpeningInstance original = OpeningInstance.builder(definition.id()).build();
		JsonCodec<OpeningInstance> codec = new OpeningInstanceCodec();
		DecodeContext context = DecodeContext.of(id -> definition.id().toString().equals(id)
			? Optional.of(definition)
			: Optional.empty());

		OpeningInstance restored = codec.fromJson(codec.toJson(original), context);

		assertEquals(original, restored);
	}

	@Test
	void missingDefinitionIsReportedThroughDiagnosticSink() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		OpeningInstanceCodec codec = new OpeningInstanceCodec();
		List<DecodeDiagnostic> diagnostics = new ArrayList<>();
		DecodeContext context = new DecodeContext(
			DefinitionResolver.empty(),
			AssetResolver.empty(),
			MigrationRegistry.empty(),
			diagnostics::add
		);

		assertThrows(IllegalArgumentException.class, () -> codec.fromJson(
			codec.toJson(OpeningInstance.builder(definition.id()).build()),
			context
		));
		assertEquals(1, diagnostics.size());
		assertEquals(DecodeDiagnostic.Severity.ERROR, diagnostics.getFirst().severity());
		assertEquals("opening-instance.decode-failed", diagnostics.getFirst().code());
	}

	@Test
	void migrationRegistryExecutesEveryVersionAndEmitsDiagnostics() {
		List<DecodeDiagnostic> diagnostics = new ArrayList<>();
		MigrationRegistry migrations = MigrationRegistry.builder()
			.register("example", 1, source -> {
				source.addProperty("migrated", true);
				return source;
			})
			.build();
		JsonObject source = new JsonObject();
		source.addProperty("schemaVersion", 1);

		JsonObject migrated = migrations.migrate("example", source, 2, diagnostics::add);

		assertEquals(2, migrated.get("schemaVersion").getAsInt());
		assertTrue(migrated.get("migrated").getAsBoolean());
		assertEquals("decode.migrated", diagnostics.getFirst().code());
		assertEquals(1, source.get("schemaVersion").getAsInt());
	}
}
