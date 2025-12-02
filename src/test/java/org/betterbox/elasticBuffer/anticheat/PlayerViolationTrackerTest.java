package org.betterbox.elasticBuffer.anticheat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerViolationTrackerTest {

    private static class MutableClock extends Clock {
        private long currentMillis;

        MutableClock(long currentMillis) {
            this.currentMillis = currentMillis;
        }

        public void advanceMillis(long delta) {
            currentMillis += delta;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public long millis() {
            return currentMillis;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentMillis);
        }
    }

    @Test
    void countsViolationsWithinWindow() {
        MutableClock clock = new MutableClock(0);
        PlayerViolationTracker tracker = new PlayerViolationTracker(clock);
        UUID player = UUID.randomUUID();
        long window = 60_000;

        tracker.addViolation(player, new ViolationRecord(CheckType.REACH, 4.0, clock.millis()), window);
        clock.advanceMillis(30_000);
        tracker.addViolation(player, new ViolationRecord(CheckType.REACH, 6.0, clock.millis()), window);

        assertEquals(2, tracker.getWindowCount(player, window, 0));

        clock.advanceMillis(40_000);
        assertEquals(1, tracker.getWindowCount(player, window, 5));
    }
}
