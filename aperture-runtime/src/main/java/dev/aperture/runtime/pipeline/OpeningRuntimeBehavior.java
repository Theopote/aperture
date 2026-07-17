package dev.aperture.runtime.pipeline;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstance;
import java.time.Instant;
import dev.aperture.core.opening.OpeningCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** First runtime behavior slice: deterministic door open/close transitions. */
public final class OpeningRuntimeBehavior implements RuntimeBehavior<OpeningInstance> {
	public static final RuntimeCapability OPEN = new RuntimeCapability("aperture:open");
	public static final RuntimeCapability CLOSE = new RuntimeCapability("aperture:close");
	public static final RuntimeCapability TOGGLE = new RuntimeCapability("aperture:toggle");
	private static final Set<RuntimeCapability> OPERABLE = Set.of(OPEN, CLOSE, TOGGLE);

	private final OpeningTypeRegistry definitions;

	public OpeningRuntimeBehavior(OpeningTypeRegistry definitions) {
		this.definitions = Objects.requireNonNull(definitions, "definitions");
	}

	@Override
	public Class<OpeningInstance> objectType() {
		return OpeningInstance.class;
	}

	@Override
	public Set<RuntimeCapability> capabilities(OpeningInstance object) {
		if (!isOperable(object) || !object.state().runtimeState().bool("enabled")) return Set.of();
		if (object.state().locked()) return Set.of(CLOSE);
		return OPERABLE;
	}


	@Override
	public RuntimeTransition<OpeningInstance> evaluate(OpeningInstance object, RuntimeInteraction interaction) {
		return evaluate(object, interaction, RuntimeEvaluationContext.empty());
	}
	@Override
	public RuntimeTransition<OpeningInstance> evaluate(
		OpeningInstance object,
		RuntimeInteraction interaction,
		RuntimeEvaluationContext context
	) {
		if (!isOperable(object)) {
			throw new IllegalArgumentException("Opening does not support runtime interaction: " + object.typeId());
		}

		double nextRatio = switch (interaction.action()) {
			case "aperture:open" -> 1.0;
			case "aperture:close" -> 0.0;
			case "aperture:toggle" -> object.state().openRatio() > 0.0 ? 0.0 : 1.0;
			default -> throw new IllegalArgumentException("Unsupported opening interaction: " + interaction.action());
		};
		if (Double.compare(nextRatio, object.state().openRatio()) == 0) {
			return RuntimeTransition.unchanged(object);
		}

		String motion = nextRatio > object.state().openRatio() ? "opening" : "closing";
		var nextState = object.state().transition(
			Map.of("openRatio", nextRatio),
			Map.of(
				"targetOpenRatio", nextRatio,
				"velocity", 0.0,
				"motion", motion,
				"activeInteractor", interaction.actor().id(),
				"lastEventTime", (double) context.tick()
			),
			Instant.EPOCH.plusMillis(context.tick())
		);
		OpeningInstance updated = object
			.withState(nextState)
			.withRevision(object.revision() + 1);
		return new RuntimeTransition<>(
			updated,
			List.of(new RuntimeEffect.GeometryInvalidated(object.instanceId()))
		);
	}

	private boolean isOperable(OpeningInstance object) {
		return definitions.require(object.typeId()).category() == OpeningCategory.DOOR;
	}
}
