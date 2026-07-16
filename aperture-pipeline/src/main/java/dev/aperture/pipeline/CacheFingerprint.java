package dev.aperture.pipeline;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

/** Deterministic fingerprints for cache key inputs. */
public final class CacheFingerprint {
	private CacheFingerprint() {
	}

	public static long definition(OpeningTypeDefinition definition) {
		String value = definition.schemaVersion() + "|" + definition.id() + "|"
			+ definition.category() + "|" + definition.parametricSchema() + "|"
			+ definition.constraints() + "|" + definition.components() + "|"
			+ definition.materialSlots();
		byte[] digest = digest(value);
		return java.nio.ByteBuffer.wrap(digest).getLong();
	}

	public static String parameters(ParameterSet parameters) {
		StringBuilder canonical = new StringBuilder();
		parameters.asMap().entrySet().stream()
			.sorted(Comparator.comparing(java.util.Map.Entry::getKey))
			.forEach(entry -> canonical.append(entry.getKey()).append('=')
				.append(value(entry.getValue())).append(';'));
		return java.util.HexFormat.of().formatHex(digest(canonical.toString()));
	}

	public static String text(Object value) {
		return java.util.HexFormat.of().formatHex(digest(String.valueOf(value)));
	}

	private static String value(ParameterValue value) {
		return value.type() + ":" + switch (value) {
			case ParameterValue.LengthValue item -> Double.toHexString(item.millimeters());
			case ParameterValue.AngleValue item -> Double.toHexString(item.degrees());
			case ParameterValue.CountValue item -> Integer.toString(item.value());
			case ParameterValue.NumberValue item -> Double.toHexString(item.value());
			case ParameterValue.EnumValue item -> item.value();
			case ParameterValue.BoolValue item -> Boolean.toString(item.value());
			case ParameterValue.MaterialRefValue item -> item.raw();
		};
	}

	private static byte[] digest(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}