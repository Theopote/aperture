package dev.aperture;

import dev.aperture.bootstrap.ApertureBootstrap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aperture implements ModInitializer {
	public static final String MOD_ID = "aperture";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ApertureBootstrap bootstrap;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Aperture — Architectural Opening Design System");
		bootstrap = new ApertureBootstrap();
		bootstrap.initialize();
	}

	public static ApertureBootstrap bootstrap() {
		return bootstrap;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
