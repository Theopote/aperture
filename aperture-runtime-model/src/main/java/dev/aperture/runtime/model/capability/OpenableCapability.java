package dev.aperture.runtime.model.capability;

/** Observable open/close semantics; changes are requested through Commands. */
public interface OpenableCapability extends Capability {
	double currentRatio();
	double targetRatio();
	boolean canRequestOpen();
	boolean canRequestClose();
}
