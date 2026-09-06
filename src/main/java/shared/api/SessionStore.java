package shared.api;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Opaque browser sessions. No credentials or balances are held in cookies. */
public final class SessionStore {
    private static final long TTL = Duration.ofMinutes(30).toMillis();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Clock clock;

    public SessionStore() { this(Clock.systemUTC()); }
    public SessionStore(Clock clock) { this.clock = clock; }

    public synchronized Session open(String token) {
        long now = clock.millis();
        sessions.values().removeIf(s -> now - s.lastSeen >= TTL);
        Session session = token == null ? null : sessions.get(token);
        if (session == null) {
            if (sessions.size() >= 10_000) throw new IllegalStateException("Server is busy. Try again later.");
            session = new Session(newToken(), now);
            sessions.put(session.token, session);
        }
        session.lastSeen = now;
        return session;
    }

    public synchronized void rotate(Session session) {
        sessions.remove(session.token);
        session.token = newToken();
        sessions.put(session.token, session);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static final class Session {
        private String token;
        private long lastSeen;
        private String role;
        private String identity;
        private final Map<String, Integer> failures = new HashMap<>();

        private Session(String token, long now) { this.token = token; lastSeen = now; }
        public String token() { return token; }
        public String role() { return role; }
        public String identity() { return identity; }
        public int remaining(String role) { return Math.max(0, 3 - failures.getOrDefault(role, 0)); }
        public void fail(String role) { failures.merge(role, 1, Integer::sum); }
        public void login(String role, String identity) { this.role = role; this.identity = identity; }
        public void logout() { role = null; identity = null; }
    }
}
