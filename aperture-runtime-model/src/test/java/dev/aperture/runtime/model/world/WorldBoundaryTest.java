package dev.aperture.runtime.model.world;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.event.SpatialRef;
import dev.aperture.runtime.model.event.WorldRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldBoundaryTest {
	@Test
	void executesTypedQueryWithoutPlatformTypes() {
		WorldQueryExecutor executor = new WorldQueryExecutor() {
			@Override
			public <R> Optional<R> execute(WorldQueryRequest<R> query) {
				if (query instanceof GetRedstoneSignalQuery) return Optional.of(query.resultType().cast(12));
				return Optional.empty();
			}
		};
		GetRedstoneSignalQuery query = new GetRedstoneSignalQuery(
			new SpatialRef(new WorldRef("test:world"), Vec3d.ZERO, "control_input"));
		assertEquals(12, executor.execute(query).orElseThrow());
		assertInstanceOf(Integer.class, executor.execute(query).orElseThrow());
	}

	@Test
	void snapshotAndQueriesOwnImmutablePlatformNeutralData() {
		WorldSnapshot snapshot = new WorldSnapshot(
			new WorldRef("test:world"), Instant.EPOCH, 2, Map.of(), Map.of("weather", "clear"));
		assertEquals("clear", snapshot.environment().get("weather"));
		assertThrows(UnsupportedOperationException.class, () -> snapshot.environment().put("weather", "rain"));
		IsAreaClearQuery query = new IsAreaClearQuery(
			new WorldRef("test:world"), BoundingBox.fromSize(1, 1, 1));
		assertEquals(Boolean.class, query.resultType());
	}

	@Test
	void validatesEffectArguments() {
		SpatialRef location = new SpatialRef(new WorldRef("test:world"), Vec3d.ZERO, "panel");
		assertThrows(IllegalArgumentException.class, () -> new SpawnParticleEffect(location, "test:dust", 0));
		assertThrows(IllegalArgumentException.class, () -> new PlaySoundEffect(location, "test:door", -1, 1));
	}
}
