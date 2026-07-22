package dev.aperture.client.editor;

import dev.aperture.editor.interaction.PickContext;
import dev.aperture.editor.model.read.EditorReadModel;
import dev.aperture.editor.model.selection.SelectionModel;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.Optional;

/** Applies world-picking results to the editor's shared selection model. */
public final class WorldSelectionController {
	private final SelectionModel selection;
	private final EditorReadModel readModel;
	private final ArchitecturalPickingService picking;

	public WorldSelectionController(SelectionModel selection, EditorReadModel readModel,
		ArchitecturalPickingService picking) {
		this.selection = Objects.requireNonNull(selection);
		this.readModel = Objects.requireNonNull(readModel);
		this.picking = Objects.requireNonNull(picking);
	}

	public boolean selectAtCrosshair(Minecraft client, boolean additive) {
		var context = new PickContext(Optional.ofNullable(selection.snapshot().primaryObject()));
		var picked = picking.pick(client, context)
			.filter(result -> readModel.object(result.objectId()).isPresent());
		if (picked.isPresent()) {
			var result = picked.get();
			if (additive) selection.addObject(result.objectId());
			else if (result.componentPath().isPresent()) {
				selection.selectComponent(result.objectId(), result.componentPath().orElseThrow());
			} else selection.selectObject(result.objectId());
			return true;
		}
		if (!additive) selection.clear();
		return false;
	}
}