package dev.aperture.fabric.serialization;

import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.persistence.ArchitecturalObjectSnapshotJsonCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/** Thin Minecraft NBT adapter around the platform-neutral canonical snapshot codec. */
public final class ArchitecturalObjectSnapshotNbtCodec {
	private static final String KEY = "architecturalObjectSnapshot";
	private static final ArchitecturalObjectSnapshotJsonCodec JSON = new ArchitecturalObjectSnapshotJsonCodec();

	private ArchitecturalObjectSnapshotNbtCodec() { }

	public static void write(ValueOutput output, ArchitecturalObjectSnapshot snapshot) {
		output.putString(KEY, JSON.encode(snapshot));
	}

	public static Optional<ArchitecturalObjectSnapshot> read(ValueInput input) {
		return input.getString(KEY).map(JSON::decode);
	}
}
