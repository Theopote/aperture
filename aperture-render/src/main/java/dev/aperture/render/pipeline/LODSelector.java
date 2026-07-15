package dev.aperture.render.pipeline;

import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshSection;

/**
 * Selects LOD tier from camera distance in meters.
 */
public final class LODSelector {
	private final double fullDistance;
	private final double mediumDistance;
	private final double lowDistance;

	public LODSelector() {
		this(16.0, 48.0, 96.0);
	}

	public LODSelector(double fullDistance, double mediumDistance, double lowDistance) {
		this.fullDistance = fullDistance;
		this.mediumDistance = mediumDistance;
		this.lowDistance = lowDistance;
	}

	public LODLevel select(double distanceMeters) {
		if (distanceMeters <= fullDistance) {
			return LODLevel.FULL;
		}
		if (distanceMeters <= mediumDistance) {
			return LODLevel.MEDIUM;
		}
		if (distanceMeters <= lowDistance) {
			return LODLevel.LOW;
		}
		return LODLevel.IMPOSTOR;
	}

	public MeshAsset resolveAsset(MeshAsset full, MeshAsset medium, MeshAsset low, MeshAsset impostor, double distanceMeters) {
		return switch (select(distanceMeters)) {
			case FULL -> full;
			case MEDIUM -> medium != null ? medium : full;
			case LOW -> low != null ? low : full;
			case IMPOSTOR -> impostor != null ? impostor : full;
		};
	}
}
