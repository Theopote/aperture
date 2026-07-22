package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.interaction.WorldRay;
import dev.aperture.editor.interaction.ScreenPoint;
import dev.aperture.math.Vec3d;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Samples host input once per client tick and translates it into editor semantics. */
public final class EditorInputBridge {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;
	private final int[] windowWidth = new int[1];
	private final int[] windowHeight = new int[1];
	private final int[] framebufferWidth = new int[1];
	private final int[] framebufferHeight = new int[1];
	private boolean previousPrimaryDown;
	private boolean previousCancelDown;

	public EditorInputFrame capture(Minecraft client) {
		long window = client.getWindow().handle();
		boolean primaryDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean cancelDown = keyDown(window, GLFW.GLFW_KEY_ESCAPE);
		boolean worldAllowed = client.screen == null && client.player != null && client.level != null;
		WorldRay ray = null;
		if (worldAllowed) {
			var origin = client.gameRenderer.getMainCamera().position();
			var direction = client.player.getViewVector(1.0F).normalize();
			ray = new WorldRay(new Vec3d(origin.x * MILLIMETERS_PER_BLOCK,
				origin.y * MILLIMETERS_PER_BLOCK, origin.z * MILLIMETERS_PER_BLOCK),
				new Vec3d(direction.x, direction.y, direction.z));
		}
		GLFW.glfwGetWindowSize(window, windowWidth, windowHeight);
		GLFW.glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
		double cursorX = scaleCursor(client.mouseHandler.xpos(), windowWidth[0], framebufferWidth[0]);
		double cursorY = scaleCursor(client.mouseHandler.ypos(), windowHeight[0], framebufferHeight[0]);
		EditorInputFrame frame = new EditorInputFrame(
			primaryDown && !previousPrimaryDown,
			primaryDown,
			!primaryDown && previousPrimaryDown,
			cancelDown && !previousCancelDown,
			keyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT),
			keyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || keyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL),
			keyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || keyDown(window, GLFW.GLFW_KEY_RIGHT_ALT),
			worldAllowed,
			new ScreenPoint(cursorX, cursorY),
			ray
		);
		previousPrimaryDown = primaryDown;
		previousCancelDown = cancelDown;
		return frame;
	}

	private static double scaleCursor(double value, int windowSize, int framebufferSize) {
		return windowSize > 0 && framebufferSize > 0 ? value * framebufferSize / windowSize : value;
	}

	private static boolean keyDown(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
}
