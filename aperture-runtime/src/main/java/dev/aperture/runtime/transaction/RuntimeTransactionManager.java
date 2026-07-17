package dev.aperture.runtime.transaction;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Serializes state transitions on bounded object lock stripes. */
public final class RuntimeTransactionManager {
	private static final int DEFAULT_STRIPES = 256;
	private final Object[] locks;

	public RuntimeTransactionManager() {
		locks = new Object[DEFAULT_STRIPES];
		java.util.Arrays.setAll(locks, ignored -> new Object());
	}

	public <T> T execute(UUID objectId, Supplier<T> transaction) {
		Objects.requireNonNull(objectId, "objectId");
		Objects.requireNonNull(transaction, "transaction");
		Object lock = locks[Math.floorMod(objectId.hashCode(), locks.length)];
		synchronized (lock) {
			return transaction.get();
		}
	}
}
