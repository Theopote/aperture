package dev.aperture.core.instance;

import dev.aperture.core.object.ArchitecturalObjectId;
import dev.aperture.math.LocalFrame;
import dev.aperture.parameter.ParameterSet;
import java.util.Objects;

/** Structured dependency on a feature of an architectural host. */
public record HostBinding(
	ArchitecturalObjectId hostId,
	HostFeatureId featureId,
	LocalFrame insertionFrame,
	HostAttachmentMode mode,
	ParameterSet attachmentParameters,
	long hostRevision
) {
	private static final String SEP = "|";

	public HostBinding {
		Objects.requireNonNull(hostId, "hostId");
		Objects.requireNonNull(featureId, "featureId");
		Objects.requireNonNull(insertionFrame, "insertionFrame");
		Objects.requireNonNull(mode, "mode");
		Objects.requireNonNull(attachmentParameters, "attachmentParameters");
		if (hostRevision < 0) throw new IllegalArgumentException("hostRevision must be non-negative");
		if ((mode == HostAttachmentMode.FREE_STANDING) != hostId.isNone()) throw new IllegalArgumentException("Host id and mode disagree");
	}

	public HostBinding(HostType type, String anchor) {
		this(type == HostType.FREE_STANDING ? ArchitecturalObjectId.NONE : legacyId(type, anchor),
			legacyFeature(type, anchor), LocalFrame.identity(),
			type == HostType.FREE_STANDING ? HostAttachmentMode.FREE_STANDING : HostAttachmentMode.CUT_THROUGH,
			ParameterSet.empty(), 0L);
	}

	public static HostBinding freeStanding() { return new HostBinding(HostType.FREE_STANDING, ""); }
	public static HostBinding wall(String anchor) { return new HostBinding(HostType.WALL, anchor); }
	public boolean sameHostFeature(HostBinding other) { return other != null && hostId.equals(other.hostId) && featureId.equals(other.featureId); }
	public String stableKey() { return hostId + "/" + featureId.type() + "/" + featureId.value(); }

	@Deprecated public String anchor() {
		int split = featureId.value().indexOf(SEP);
		return featureId.type() == HostFeatureType.NAMED_ANCHOR && split >= 0 ? featureId.value().substring(split + 1) : featureId.value();
	}

	@Deprecated public HostType type() {
		if (mode == HostAttachmentMode.FREE_STANDING) return HostType.FREE_STANDING;
		int split = featureId.value().indexOf(SEP);
		if (featureId.type() == HostFeatureType.NAMED_ANCHOR && split > 0) {
			try { return HostType.fromId(featureId.value().substring(0, split)); }
			catch (IllegalArgumentException ignored) { }
		}
		return HostType.WALL;
	}

	private static ArchitecturalObjectId legacyId(HostType type, String anchor) {
		return ArchitecturalObjectId.deterministic("legacy-host:" + type.id() + ":" + Objects.requireNonNull(anchor));
	}

	private static HostFeatureId legacyFeature(HostType type, String anchor) {
		String value = Objects.requireNonNull(anchor);
		return HostFeatureId.namedAnchor(type.id() + SEP + (value.isBlank() ? "default" : value));
	}
}
