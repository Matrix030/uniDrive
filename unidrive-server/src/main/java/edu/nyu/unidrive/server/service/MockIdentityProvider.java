package edu.nyu.unidrive.server.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class MockIdentityProvider {

    private static final Map<String, MockAccount> ACCOUNTS = Map.of(
        "student@nyu.edu", new MockAccount("rvg9395", "Student Demo", "student@nyu.edu", "STUDENT", "password123"),
        "rvg9395@nyu.edu", new MockAccount("rvg9395", "Rishikesh", "rvg9395@nyu.edu", "STUDENT", "password123"),
        "instructor@nyu.edu", new MockAccount("instructor_rvg0000", "Instructor Demo", "instructor@nyu.edu", "INSTRUCTOR", "password123"),
        "rvg0000@nyu.edu", new MockAccount("instructor_rvg0000", "Instructor Demo", "rvg0000@nyu.edu", "INSTRUCTOR", "password123")
    );

    public Optional<AuthenticatedUser> authenticate(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        MockAccount account = ACCOUNTS.get(email.trim().toLowerCase(Locale.ROOT));
        if (account == null || !account.password().equals(password)) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(
            account.userId(),
            account.name(),
            account.email(),
            account.role(),
            "mock-sso-token-" + account.userId()
        ));
    }

    public record AuthenticatedUser(String userId, String name, String email, String role, String accessToken) {
    }

    private record MockAccount(String userId, String name, String email, String role, String password) {
    }
}
