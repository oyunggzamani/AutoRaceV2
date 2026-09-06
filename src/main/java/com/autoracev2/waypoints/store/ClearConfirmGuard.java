package com.autoracev2.waypoints.store;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-step confirmation for destructive clear commands.
 */
public final class ClearConfirmGuard {
    public enum Outcome {
        REQUESTED,
        CONFIRMED,
        EXPIRED,
        NOT_PENDING
    }

    private final Map<String, Long> pendingUntil = new ConcurrentHashMap<>();
    private final long timeoutMillis;
    private final Clock clock;

    public ClearConfirmGuard(long timeoutMillis) {
        this(timeoutMillis, System::currentTimeMillis);
    }

    public ClearConfirmGuard(long timeoutMillis, Clock clock) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
        this.clock = clock;
    }

    public Outcome request(String actorId) {
        pendingUntil.put(requireActor(actorId), clock.now() + timeoutMillis);
        return Outcome.REQUESTED;
    }

    public Outcome confirm(String actorId) {
        String key = requireActor(actorId);
        Long until = pendingUntil.remove(key);
        if (until == null) {
            return Outcome.NOT_PENDING;
        }
        if (clock.now() > until) {
            return Outcome.EXPIRED;
        }
        return Outcome.CONFIRMED;
    }

    public boolean isPending(String actorId) {
        Long until = pendingUntil.get(requireActor(actorId));
        return until != null && clock.now() <= until;
    }

    public void cancel(String actorId) {
        pendingUntil.remove(requireActor(actorId));
    }

    public void purgeExpired() {
        long now = clock.now();
        Iterator<Map.Entry<String, Long>> iterator = pendingUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
            }
        }
    }

    private static String requireActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        return actorId;
    }

    @FunctionalInterface
    public interface Clock {
        long now();
    }
}
