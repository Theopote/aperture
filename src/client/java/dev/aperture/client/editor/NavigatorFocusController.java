package dev.aperture.client.editor;

import dev.aperture.editor.imgui.NavigatorFocusRequests;
import dev.aperture.editor.model.session.EditorSession;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;

/** Applies Navigator focus intents by orienting the local camera toward the selected object. */
final class NavigatorFocusController {
	private NavigatorFocusController() { }

	static void update(EditorSession session) {
		NavigatorFocusRequests.consume().flatMap(session.readModel()::object).ifPresent(view -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) return;
			client.player.lookAt(EntityAnchorArgument.Anchor.EYES, ClientWorldUnits.toBlocks(view.transform().origin()));
		});
	}
}
