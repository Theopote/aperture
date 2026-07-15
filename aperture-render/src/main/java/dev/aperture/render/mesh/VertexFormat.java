package dev.aperture.render.mesh;

/**
 * CPU-side vertex layout for mesh compilation.
 * Position (3) + normal (3) + uv (2) = 8 floats per vertex.
 */
public final class VertexFormat {
	public static final int FLOATS_PER_VERTEX = 8;
	public static final int POSITION_OFFSET = 0;
	public static final int NORMAL_OFFSET = 3;
	public static final int UV_OFFSET = 6;

	private VertexFormat() {
	}

	public static int vertexCount(float[] vertices) {
		if (vertices.length % FLOATS_PER_VERTEX != 0) {
			throw new IllegalArgumentException("vertices length must be a multiple of " + FLOATS_PER_VERTEX);
		}
		return vertices.length / FLOATS_PER_VERTEX;
	}
}
