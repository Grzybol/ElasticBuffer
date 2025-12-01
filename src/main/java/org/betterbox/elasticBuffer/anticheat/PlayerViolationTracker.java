package org.betterbox.elasticBuffer.anticheat;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains rolling violation history per player with expiration.
 */
public class PlayerViolationTracker {
    private final Map<UUID, Deque<ViolationRecord>> violationsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> totalCountByPlayer = new HashMap<>();
    private final Clock clock;

    public PlayerViolationTracker() {
        this(Clock.systemUTC());
    }

    public PlayerViolationTracker(Clock clock) {
        this.clock = clock;
    }

    public void addViolation(UUID playerId, ViolationRecord record, long maxHistoryMillis) {
        Deque<ViolationRecord> deque = violationsByPlayer.computeIfAbsent(playerId, uuid -> new ArrayDeque<>());
        deque.addLast(record);
        totalCountByPlayer.merge(playerId, 1, Integer::sum);
        purgeExpired(deque, maxHistoryMillis);
    }

    public int getWindowCount(UUID playerId, long windowMillis, double severityMin) {
        Deque<ViolationRecord> deque = violationsByPlayer.get(playerId);
        if (deque == null) {
            return 0;
        }
        purgeExpired(deque, windowMillis);
        int count = 0;
        long cutoff = clock.millis() - windowMillis;
        for (ViolationRecord record : deque) {
            if (record.getTimestamp() >= cutoff && record.getSeverity() >= severityMin) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount(UUID playerId) {
        return totalCountByPlayer.getOrDefault(playerId, 0);
    }

    public List<ViolationRecord> getRecentViolations(UUID playerId, long windowMillis) {
        Deque<ViolationRecord> deque = violationsByPlayer.get(playerId);
        if (deque == null) {
            return List.of();
        }
        purgeExpired(deque, windowMillis);
        long cutoff = clock.millis() - windowMillis;
        List<ViolationRecord> copy = new ArrayList<>();
        for (ViolationRecord record : deque) {
            if (record.getTimestamp() >= cutoff) {
                copy.add(record);
            }
        }
        return copy;
    }

    private void purgeExpired(Deque<ViolationRecord> deque, long windowMillis) {
        if (windowMillis <= 0) {
            deque.clear();
            return;
        }
        long cutoff = clock.millis() - windowMillis;
        while (!deque.isEmpty() && deque.peekFirst().getTimestamp() < cutoff) {
            deque.removeFirst();
        }
    }
}
