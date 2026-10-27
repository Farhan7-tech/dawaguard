package in.yusr.dawaguard.web;

import in.yusr.dawaguard.config.DawaGuardProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every check spends Nebius and Tavily credits, and the demo is public until judging ends.
 * Caps checks per client per hour and in total per day. In-memory, which is enough for a single instance.
 */
@Component
public class RateLimiter {

    private final DawaGuardProperties.Limits limits;
    private final Map<String, Deque<Instant>> perClient = new ConcurrentHashMap<>();
    private final Deque<Instant> global = new ArrayDeque<>();

    public RateLimiter(DawaGuardProperties props) {
        this.limits = props.limits();
    }

    public synchronized boolean tryAcquire(String client) {
        Instant now = Instant.now();
        prune(global, now.minus(Duration.ofDays(1)));
        Deque<Instant> mine = perClient.computeIfAbsent(client, k -> new ArrayDeque<>());
        prune(mine, now.minus(Duration.ofHours(1)));
        if (global.size() >= limits.checksPerDay() || mine.size() >= limits.checksPerClientPerHour()) {
            return false;
        }
        global.addLast(now);
        mine.addLast(now);
        return true;
    }

    private static void prune(Deque<Instant> times, Instant cutoff) {
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.pollFirst();
        }
    }
}
