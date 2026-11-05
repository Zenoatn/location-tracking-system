package br.edu.maua.server.services;

import br.edu.maua.common.security.CryptoUtil;
import br.edu.maua.datamanager.dao.UserDao;
import br.edu.maua.model.UserCredential;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

public class AuthService {

    private static final String DUMMY_PASSWORD_HASH = createDummyPasswordHash();

    private final UserDao userDao;
    private final SessionRegistry sessionRegistry;

    public AuthService() {
        this(new UserDao(), SessionRegistry.getInstance());
    }

    AuthService(UserDao userDao, SessionRegistry sessionRegistry) {
        this.userDao = userDao;
        this.sessionRegistry = sessionRegistry;
    }

    public LoginSession login(String email, char[] password)
            throws SQLException, GeneralSecurityException {
        try {
            Optional<UserCredential> candidate = userDao.findActiveByEmail(email.trim());
            String storedHash = candidate.map(UserCredential::getPasswordHash)
                    .orElse(DUMMY_PASSWORD_HASH);
            boolean passwordMatches = CryptoUtil.verifyPassword(password, storedHash);
            if (candidate.isEmpty() || !passwordMatches) {
                return null;
            }
            UserCredential user = candidate.get();
            return new LoginSession(user.getId(), user.getName(), user.getRole(),
                    sessionRegistry.create(user));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public record LoginSession(long userId, String name, String role, String token) {
    }

    private static String createDummyPasswordHash() {
        char[] dummyPassword = "not-a-real-user-password".toCharArray();
        try {
            return CryptoUtil.hashPassword(dummyPassword);
        } catch (GeneralSecurityException exception) {
            throw new ExceptionInInitializerError(exception);
        } finally {
            java.util.Arrays.fill(dummyPassword, '\0');
        }
    }
}
