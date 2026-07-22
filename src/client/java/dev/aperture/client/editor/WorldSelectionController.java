package dev.aperture.client.editor;

import dev.aperture.editor.model.read.EditorReadModel;
import dev.aperture.editor.model.selection.SelectionModel;
import net.minecraft.client.Minecraft;

import java.util.Objects;

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
		var picked = picking.pick(client).filter(id -> readModel.object(id).isPresent());
		if (picked.isPresent()) {
			if (additive) selection.addObject(picked.get());
			else selection.selectObject(picked.get());
			return true;
		}
		if (!additive) selection.clear();
		return false;
	}
}
