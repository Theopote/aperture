package dev.aperture.kernel;

import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** K1 production baselines for every frozen reference family. */
class KernelFamilyBaselineTest {
	@Test
	void frozenFamiliesAreDeterministicThroughKernel() {
		try (ApertureKernel kernel = ApertureKernel.builder().withCacheCapacity(32).build()) {
			assertDeterministic(kernel, "aperture:door", Map.of("width", 900.0, "height", 2100.0));
			assertDeterministic(kernel, "aperture:fixed_window", Map.of("width", 1200.0, "height", 1500.0));
			assertDeterministic(kernel, "aperture:curtain_wall", Map.of("width", 3600.0, "height", 3000.0));
		}
	}

	private static void assertDeterministic(ApertureKernel kernel, String typeId, Map<String, Object> parameters) {
		OpeningResult.Success first = kernel.generate(typeId, parameters).asSuccess();
		OpeningResult.Success second = kernel.generate(typeId, parameters).asSuccess();
		var left = first.output();
		var right = second.output();

		assertFalse(left.geometry().solids().isEmpty(), typeId + " geometry");
		assertFalse(left.meshes().partsByPath().isEmpty(), typeId + " meshes");
		assertEquals(componentPaths(left), componentPaths(right), typeId + " component paths");
		assertEquals(vertexCount(left), vertexCount(right), typeId + " vertex count");
		assertEquals(triangleCount(left), triangleCount(right), typeId + " triangle count");
		assertEquals(left.geometry().bounds(), right.geometry().bounds(), typeId + " bounds");
		assertEquals(materialKeys(left), materialKeys(right), typeId + " material keys");
		assertEquals(
			GeometryRecipeCodec.toJson(left.recipe()),
			GeometryRecipeCodec.toJson(right.recipe()),
			typeId + " recipe fingerprint source"
		);
	}

	private static Set<String> componentPaths(dev.aperture.geometry.pipeline.PipelineResult result) {
		return new TreeSet<>(result.meshes().partsByPath().keySet());
	}

	private static Set<String> materialKeys(dev.aperture.geometry.pipeline.PipelineResult result) {
		Set<String> keys = new TreeSet<>();
		result.geometry().solids().forEach(solid -> keys.add(solid.materialSlot()));
		return keys;
	}

	private static int vertexCount(dev.aperture.geometry.pipeline.PipelineResult result) {
		return result.meshes().partsByPath().values().stream().mapToInt(mesh -> mesh.vertexCount()).sum();
	}

	private static int triangleCount(dev.aperture.geometry.pipeline.PipelineResult result) {
		return result.meshes().partsByPath().values().stream().mapToInt(mesh -> mesh.triangleCount()).sum();
	}
}