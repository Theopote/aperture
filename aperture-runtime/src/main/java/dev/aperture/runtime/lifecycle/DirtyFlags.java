package dev.aperture.runtime.lifecycle;

/** Commit-derived work that platform adapters still need to consume. */
public record DirtyFlags(boolean persistence, boolean replication, boolean kinematics) {
	public static final DirtyFlags CLEAN = new DirtyFlags(false, false, false);
	public static final DirtyFlags CREATED = new DirtyFlags(true, true, true);
}
