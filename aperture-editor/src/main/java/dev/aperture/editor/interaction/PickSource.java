package dev.aperture.editor.interaction;

import java.util.List;

public interface PickSource {
	String id();
	List<PickCandidate> pick(WorldRay ray, PickContext context);
}
