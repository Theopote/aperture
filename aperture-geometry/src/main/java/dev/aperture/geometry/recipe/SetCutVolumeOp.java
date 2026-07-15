package dev.aperture.geometry.recipe;

import dev.aperture.math.BoundingBox;

public record SetCutVolumeOp(BoundingBox cutVolume) implements GeometryOp {
}
