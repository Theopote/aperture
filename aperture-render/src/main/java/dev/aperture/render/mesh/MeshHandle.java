package dev.aperture.render.mesh;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Opaque reference linking a render part to a compiled mesh section in a backend cache.
 */
public record MeshHandle(long id) {
	private static final AtomicLong NEXT_ID = new AtomicLong(1);

	public static MeshHandle next() {
		return new MeshHandle(NEXT_ID.getAndIncrement());
	}
}
