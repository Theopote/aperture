package dev.aperture.geometry.material;

import dev.aperture.geometry.model.PartId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable snapshot of material bindings for one geometry revision.
 */
public final class MaterialBindingSet {
	private final Map<PartId, MaterialBinding> byPart;

	public MaterialBindingSet(Map<PartId, MaterialBinding> byPart) {
		this.byPart = Collections.unmodifiableMap(new LinkedHashMap<>(byPart));
	}

	public Optional<MaterialBinding> get(PartId partId) {
		return Optional.ofNullable(byPart.get(partId));
	}

	public Set<PartId> partIds() {
		return byPart.keySet();
	}

	public MaterialBindingSet filtered(Predicate<MaterialBinding> predicate) {
		Map<PartId, MaterialBinding> filtered = new LinkedHashMap<>();
		for (Map.Entry<PartId, MaterialBinding> entry : byPart.entrySet()) {
			if (predicate.test(entry.getValue())) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}
		return new MaterialBindingSet(filtered);
	}
}
