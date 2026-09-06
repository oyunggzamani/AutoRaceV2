package com.autoracev2.waypoints;

import com.autoracev2.waypoints.store.ClearConfirmGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearConfirmGuardTest {
    @Test
    void confirmRequiresPriorRequest() {
        MutableClock clock = new MutableClock(1_000L);
        ClearConfirmGuard guard = new ClearConfirmGuard(30_000L, clock);

        assertEquals(ClearConfirmGuard.Outcome.NOT_PENDING, guard.confirm("admin"));
        assertEquals(ClearConfirmGuard.Outcome.REQUESTED, guard.request("admin"));
        assertTrue(guard.isPending("admin"));
        assertEquals(ClearConfirmGuard.Outcome.CONFIRMED, guard.confirm("admin"));
        assertFalse(guard.isPending("admin"));
        assertEquals(ClearConfirmGuard.Outcome.NOT_PENDING, guard.confirm("admin"));
    }

    @Test
    void expiredRequestIsRejected() {
        MutableClock clock = new MutableClock(0L);
        ClearConfirmGuard guard = new ClearConfirmGuard(30_000L, clock);
        guard.request("admin");
        clock.now = 30_001L;
        assertEquals(ClearConfirmGuard.Outcome.EXPIRED, guard.confirm("admin"));
        assertFalse(guard.isPending("admin"));
    }

    @Test
    void actorsAreIsolated() {
        ClearConfirmGuard guard = new ClearConfirmGuard(30_000L);
        guard.request("one");
        assertEquals(ClearConfirmGuard.Outcome.NOT_PENDING, guard.confirm("two"));
        assertEquals(ClearConfirmGuard.Outcome.CONFIRMED, guard.confirm("one"));
    }

    private static final class MutableClock implements ClearConfirmGuard.Clock {
        private long now;

        private MutableClock(long now) {
            this.now = now;
        }

        @Override
        public long now() {
            return now;
        }
    }
}
