package dev.aperture.core.instance;

import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;

import java.util.Objects;
import java.util.UUID;

/**
 * A placed opening in the world (Revit "instance").
 */
public record OpeningInstance(
	int schemaVersion,
	UUID instanceId,
	OpeningId typeId,
	ParameterSet parameters,
	Transform3d transform,
	HostBinding host,
	OpeningState state,
	long revision
) {
	public OpeningInstance {
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be >= 1");
		}
		Objects.requireNonNull(instanceId, "instanceId");
		Objects.requireNonNull(typeId, "typeId");
		Objects.requireNonNull(parameters, "parameters");
		Objects.requireNonNull(transform, "transform");
		Objects.requireNonNull(host, "host");
		Objects.requireNonNull(state, "state");
		if (revision < 0) {
			throw new IllegalArgumentException("revision must be non-negative");
		}
	}

	public static Builder builder(OpeningId typeId) {
		return new Builder(typeId);
	}

	public OpeningInstance withRevision(long newRevision) {
		return new OpeningInstance(
			schemaVersion,
			instanceId,
			typeId,
			parameters,
			transform,
			host,
			state,
			newRevision
		);
	}

	public static final class Builder {
		private final OpeningId typeId;
		private UUID instanceId = UUID.randomUUID();
		private int schemaVersion = 1;
		private ParameterSet parameters = ParameterSet.empty();
		private Transform3d transform = Transform3d.at(0, 0, 0, dev.aperture.core.geometry.Facing.NORTH);
		private HostBinding host = HostBinding.freeStanding();
		private OpeningState state = OpeningState.CLOSED;
		private long revision;

		private Builder(OpeningId typeId) {
			this.typeId = typeId;
		}

		public Builder instanceId(UUID instanceId) {
			this.instanceId = instanceId;
			return this;
		}

		public Builder parameters(ParameterSet parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder transform(Transform3d transform) {
			this.transform = transform;
			return this;
		}

		public Builder host(HostBinding host) {
			this.host = host;
			return this;
		}

		public Builder state(OpeningState state) {
			this.state = state;
			return this;
		}

		public Builder revision(long revision) {
			this.revision = revision;
			return this;
		}

		public OpeningInstance build() {
			return new OpeningInstance(
				schemaVersion,
				instanceId,
				typeId,
				parameters,
				transform,
				host,
				state,
				revision
			);
		}
	}
}
