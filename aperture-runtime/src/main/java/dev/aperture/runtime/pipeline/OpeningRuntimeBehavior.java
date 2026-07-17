package dev.aperture.runtime.pipeline;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningCategory;

import java.util.List;
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
		return isOperable(object) ? OPERABLE : Set.of();
	}

	@Override
	public RuntimeTransition<OpeningInstance> evaluate(
		OpeningInstance object,
		RuntimeInteraction interaction
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

		OpeningInstance updated = object
			.withState(new OpeningState(nextRatio))
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
