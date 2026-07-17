package dev.aperture.runtime.transaction;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/** Serializes state transitions per object while allowing unrelated objects to run concurrently. */
public final class RuntimeTransactionManager {
	private final ConcurrentMap<UUID, Object> locks = new ConcurrentHashMap<>();

	public <T> T execute(UUID objectId, Supplier<T> transaction) {
		Objects.requireNonNull(objectId, "objectId");
		Objects.requireNonNull(transaction, "transaction");
		Object lock = locks.computeIfAbsent(objectId, ignored -> new Object());
		synchronized (lock) {
			return transaction.get();
		}
	}
}
