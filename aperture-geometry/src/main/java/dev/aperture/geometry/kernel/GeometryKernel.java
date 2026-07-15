package dev.aperture.geometry.kernel;

/**
 * Entry marker for the domain-agnostic geometry kernel.
 *
 * <p>The kernel knows profiles, extrusion, booleans, and meshing — not Door, Window, or CurtainWall.
 * Opening-specific assembly lives in {@code aperture-opening-geometry}.
 *
 * <ul>
 *   <li>{@link dev.aperture.geometry.profile.ProfileCurve} — 2D profiles (rectangle, polyline, arc)</li>
 *   <li>{@link dev.aperture.math.Vec2d} / {@link dev.aperture.math.Vec3d} — millimeter-space vectors</li>
 *   <li>{@link dev.aperture.geometry.ops.ExtrudeOp} — linear extrusion / sweep</li>
 *   <li>{@link dev.aperture.geometry.ops.BooleanOp} — CSG union / subtract</li>
 *   <li>{@link dev.aperture.geometry.mesh.ShapeMesher} — solid → triangle mesh</li>
 *   <li>{@link dev.aperture.geometry.recipe.GeometryRecipe} — declarative op IR for kernel evaluation</li>
 *   <li>{@link dev.aperture.geometry.export.GeometryExport} — recipe JSON + glTF export</li>
 *   <li>{@link ProfileExtruder} — profile rail extrusion helper</li>
 * </ul>
 */
public final class GeometryKernel {
	private GeometryKernel() {
	}
}
