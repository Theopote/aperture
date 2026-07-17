package dev.aperture.core.serialization;

/**
 * Platform-neutral JSON serialization contract.
 */
public interface JsonCodec<T> {
	String toJson(T value);

	T fromJson(String json, DecodeContext context);
}
