package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Samples host input once per client tick and translates it into editor semantics. */
public final class EditorInputBridge {
	private boolean previousPrimaryDown;
	private boolean previousCancelDown;

	public EditorInputFrame capture(Minecraft client) {
		long window = client.getWindow().handle();
		boolean primaryDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean cancelDown = keyDown(window, GLFW.GLFW_KEY_ESCAPE);
		boolean worldAllowed = client.screen == null && client.player != null && client.level != null;
		EditorInputFrame.WorldRay ray = null;
		if (worldAllowed) {
			var origin = client.gameRenderer.getMainCamera().position();
			var direction = client.player.getViewVector(1.0F).normalize();
			ray = new EditorInputFrame.WorldRay(origin.x, origin.y, origin.z,
				direction.x, direction.y, direction.z);
		}
		EditorInputFrame frame = new EditorInputFrame(
			primaryDown && !previousPrimaryDown,
			primaryDown,
			!primaryDown && previousPrimaryDown,
			cancelDown && !previousCancelDown,
			keyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT),
			keyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || keyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL),
			keyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || keyDown(window, GLFW.GLFW_KEY_RIGHT_ALT),
			worldAllowed,
			ray
		);
		previousPrimaryDown = primaryDown;
		previousCancelDown = cancelDown;
		return frame;
	}

	private static boolean keyDown(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
}
