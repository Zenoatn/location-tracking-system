package br.edu.maua.server.services;

import br.edu.maua.model.UserCredential;
import br.edu.maua.model.UserSession;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SessionRegistry {

    private static final SessionRegistry INSTANCE = new SessionRegistry();
    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    private SessionRegistry() {
    }

    public static SessionRegistry getInstance() {
        return INSTANCE;
    }

    public String create(UserCredential user) {
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().getExpiresAt().isAfter(now));
        sessions.put(token, new UserSession(user.getId(), user.getName(), user.getRole(),
                now.plus(SESSION_LIFETIME)));
        return token;
    }

    public UserSession find(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        UserSession session = sessions.get(token);
        if (session != null && !session.getExpiresAt().isAfter(Instant.now())) {
            sessions.remove(token, session);
            return null;
        }
        return session;
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
