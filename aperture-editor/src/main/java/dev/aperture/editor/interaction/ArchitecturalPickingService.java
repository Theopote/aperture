package dev.aperture.editor.interaction;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Combines independent pick sources using deterministic priority-then-distance arbitration. */
public final class ArchitecturalPickingService {
	private final List<PickSource> sources;

	public ArchitecturalPickingService(List<PickSource> sources) {
		this.sources = List.copyOf(sources);
		if (this.sources.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("null pick source");
	}

	public Optional<PickResult> pick(WorldRay ray, PickContext context) {
		return sources.stream()
			.flatMap(source -> source.pick(ray, context).stream().map(candidate -> new Sourced(source.id(), candidate)))
			.min(Comparator.<Sourced>comparingInt(value -> -value.candidate().priority().rank())
				.thenComparingDouble(value -> value.candidate().distance())
				.thenComparing(Sourced::sourceId))
			.map(value -> {
				PickCandidate hit = value.candidate();
				return new PickResult(hit.objectId(), hit.componentPath(), hit.manipulatorId(), hit.hitKind(),
					hit.worldPosition(), hit.worldNormal(), hit.distance(), hit.priority(), value.sourceId());
			});
	}

	private record Sourced(String sourceId, PickCandidate candidate) { }
}
