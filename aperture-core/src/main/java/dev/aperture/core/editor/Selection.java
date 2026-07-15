package dev.aperture.core.editor;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Current editor selection — one primary object plus optional multi-select.
 */
public final class Selection {
	private final Set<EditorObjectId> selected;
	private EditorObjectId primary;

	private Selection(Set<EditorObjectId> selected, EditorObjectId primary) {
		this.selected = new LinkedHashSet<>(selected);
		this.primary = primary;
	}

	public static Selection empty() {
		return new Selection(Set.of(), null);
	}

	public static Selection of(EditorObjectId id) {
		return new Selection(Set.of(id), id);
	}

	public Selection select(EditorObjectId id) {
		Objects.requireNonNull(id, "id");
		selected.clear();
		selected.add(id);
		primary = id;
		return this;
	}

	public Selection add(EditorObjectId id) {
		Objects.requireNonNull(id, "id");
		selected.add(id);
		if (primary == null) {
			primary = id;
		}
		return this;
	}

	public Selection clear() {
		selected.clear();
		primary = null;
		return this;
	}

	public boolean isEmpty() {
		return selected.isEmpty();
	}

	public boolean contains(EditorObjectId id) {
		return selected.contains(id);
	}

	public Set<EditorObjectId> selected() {
		return Set.copyOf(selected);
	}

	public Optional<EditorObjectId> primary() {
		return Optional.ofNullable(primary);
	}
}
