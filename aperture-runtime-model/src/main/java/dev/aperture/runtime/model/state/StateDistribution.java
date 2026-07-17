package dev.aperture.runtime.model.state;

/** Network ownership and distribution policy, independent from persistence. */
public enum StateDistribution {
	LOCAL,
	REPLICATED,
	SERVER_ONLY,
	CLIENT_PREDICTED
}
