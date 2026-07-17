package dev.aperture.runtime.model.world;

/** Typed immutable request executed by a platform adapter. */
public interface WorldQueryRequest<R> {
	String queryType();
	Class<R> resultType();
}
