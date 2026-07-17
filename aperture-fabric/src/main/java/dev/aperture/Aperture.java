package dev.aperture;

import dev.aperture.bootstrap.ApertureBootstrap;
import dev.aperture.bootstrap.DevelopmentSelfTest;
import dev.aperture.fabric.network.ApertureReplicationNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aperture implements ModInitializer {
	public static final String MOD_ID = "aperture";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ApertureBootstrap bootstrap;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Aperture — Architectural Opening Design System");
		ApertureReplicationNetworking.registerPayloadType();
		bootstrap = new ApertureBootstrap();
		bootstrap.initialize();
		if (DevelopmentSelfTest.shouldRun(
			FabricLoader.getInstance().isDevelopmentEnvironment(),
			Boolean.getBoolean("aperture.developmentSelfTest")
		)) {
			DevelopmentSelfTest.run(bootstrap.openingTypes(), bootstrap.generation());
		}

		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
			Identifier.fromNamespaceAndPath(MOD_ID, "kernel_resources"),
			(ResourceManagerReloadListener) resourceManager -> bootstrap.reloadKernelResources()
		);
	}

	public static ApertureBootstrap bootstrap() {
		return bootstrap;
	}

	/** Namespaced resource id string, e.g. {@code aperture:fixed_window}. */
	public static String id(String path) {
		return MOD_ID + ":" + path;
	}
}
