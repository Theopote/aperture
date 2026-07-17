package dev.aperture.opening.runtime;

import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.geometry.kinematic.KinematicPart;
import dev.aperture.geometry.kinematic.MotionDefinition;
import dev.aperture.geometry.kinematic.MovementType;
import dev.aperture.geometry.kinematic.Pivot;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.opening.geometry.build.CompiledGeometry;

import java.util.ArrayList;

/** Door-family factory for swing-panel kinematic metadata. */
public final class DoorKinematics {
	private DoorKinematics() { }

	public static KinematicPart swingPanel(String componentPath, Vec3d hingePivot, boolean clockwise) {
		Vec3d axis = clockwise ? new Vec3d(0, 1, 0) : new Vec3d(0, -1, 0);
		return new KinematicPart(
			new ComponentPath(componentPath),
			Transform3d.identity(),
			null,
			new Pivot(hingePivot),
			new MotionDefinition(MovementType.ROTATE, axis, 0, Math.toRadians(90), DoorStateSchema.OPEN_RATIO)
		);
	}

	/** Adds runtime motion metadata without rebuilding recipe, solids, or mesh inputs. */
	public static CompiledGeometry attachSwingPanel(
		CompiledGeometry geometry, String componentPath, Vec3d hingePivot, boolean clockwise
	) {
		ArrayList<KinematicPart> parts = new ArrayList<>(geometry.kinematicParts());
		parts.add(swingPanel(componentPath, hingePivot, clockwise));
		return new CompiledGeometry(geometry.recipe(), geometry.result(), parts);
	}
}
