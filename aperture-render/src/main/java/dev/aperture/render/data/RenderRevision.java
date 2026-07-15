package dev.aperture.render.data;

/**
 * Monotonic render-side revision, distinct from {@code OpeningInstance.revision} (network sync).
 */
public record RenderRevision(long value) {
	public static final RenderRevision ZERO = new RenderRevision(0);

	public RenderRevision {
		if (value < 0) {
			throw new IllegalArgumentException("value must be non-negative");
		}
	}

	public RenderRevision next() {
		return new RenderRevision(value + 1);
	}
}
