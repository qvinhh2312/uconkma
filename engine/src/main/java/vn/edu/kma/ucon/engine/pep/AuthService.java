package vn.edu.kma.ucon.engine.pep;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pip.entity.AccountRole;
import vn.edu.kma.ucon.engine.pip.entity.UserAccount;
import vn.edu.kma.ucon.engine.pip.repository.UserAccountRepository;

@Service
public class AuthService {

    private static final String HASH_SALT = "UCONKMA_DEMO_AUTH";
    private final UserAccountRepository userAccountRepository;
    private final Map<String, AuthPrincipal> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || !hasText(request.username()) || !hasText(request.password())) {
            throw new IllegalArgumentException("username and password are required.");
        }
        UserAccount account = userAccountRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        if (!account.isEnabled() || !hashPassword(request.password()).equals(account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        String token = UUID.randomUUID() + "." + Instant.now().toEpochMilli();
        AuthPrincipal principal = toPrincipal(account);
        sessions.put(token, principal);
        return new LoginResponse(token, principal.username(), principal.displayName(), principal.role().name(),
                principal.studentId());
    }

    public Optional<AuthPrincipal> authenticate(String authorizationHeader) {
        if (!hasText(authorizationHeader)) {
            return Optional.empty();
        }
        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public AuthPrincipal requireAuthenticated(String authorizationHeader) {
        return authenticate(authorizationHeader)
                .orElseThrow(() -> new IllegalArgumentException("Authentication token is required."));
    }

    public AuthPrincipal requireAdmin(String authorizationHeader) {
        AuthPrincipal principal = requireAuthenticated(authorizationHeader);
        if (principal.role() != AccountRole.ADMIN) {
            throw new IllegalArgumentException("ADMIN role is required.");
        }
        return principal;
    }

    public void logout(String authorizationHeader) {
        if (!hasText(authorizationHeader)) {
            return;
        }
        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        sessions.remove(token);
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((HASH_SALT + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private AuthPrincipal toPrincipal(UserAccount account) {
        return new AuthPrincipal(account.getUsername(), account.getDisplayName(), account.getRole(),
                account.getStudentId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token, String username, String displayName, String role, String studentId) {}
}
