package com.relay.auth;

import com.relay.identity.AppUser;
import com.relay.identity.AppUserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auth flows: phone OTP (code stored in Redis) and a demo Google sign-in. Both find-or-create an
 * {@link AppUser} under the seeded demo org and map the session to the seeded demo workspace.
 *
 * <p>OTP delivery and Google token verification are simulated for the demo — the OTP is returned
 * in the response ({@code devCode}) instead of being texted. The JWT, gating and RLS are real.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Seeded demo tenancy (see V2__seed_dev.sql).
    private static final UUID DEMO_ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEMO_WORKSPACE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DEMO_WORKSPACE_NAME = "Northstar Plumbing";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private final AppUserRepository users;
    private final JwtService jwt;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, JwtService jwt, StringRedisTemplate redis,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwt = jwt;
        this.redis = redis;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResult register(String email, String password, String name) {
        String e = normalizeEmail(email);
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (users.findByEmail(e).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setOrgId(DEMO_ORG);
        u.setEmail(e);
        u.setName(name == null || name.isBlank() ? e.split("@")[0] : name.trim());
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setCreatedAt(OffsetDateTime.now());
        return result(users.save(u));
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        AppUser u = users.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));
        if (u.getPasswordHash() == null) {
            throw new IllegalArgumentException("This email uses Google or phone sign-in");
        }
        if (password == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect email or password");
        }
        return result(u);
    }

    private static String normalizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email");
        }
        return email.trim().toLowerCase();
    }

    /** Generate + store an OTP for a phone number. Returns the code (demo: shown to the user). */
    public String startOtp(String phone) {
        String normalized = normalizePhone(phone);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        redis.opsForValue().set(otpKey(normalized), code, OTP_TTL);
        log.info("[otp] code for {} is {}", normalized, code); // demo only — never log codes in prod
        return code;
    }

    @Transactional
    public AuthResult verifyOtp(String phone, String code) {
        String normalized = normalizePhone(phone);
        String stored = redis.opsForValue().get(otpKey(normalized));
        if (stored == null) {
            throw new IllegalArgumentException("Code expired — request a new one");
        }
        if (!stored.equals(code)) {
            throw new IllegalArgumentException("Incorrect code");
        }
        redis.delete(otpKey(normalized));
        String last4 = normalized.length() >= 4 ? normalized.substring(normalized.length() - 4) : normalized;
        AppUser user = findOrCreate(normalized + "@phone.relay.dev", "Phone ••" + last4);
        return result(user);
    }

    @Transactional
    public AuthResult googleLogin(String email, String name) {
        String e = (email == null || email.isBlank()) ? "demo.user@gmail.com" : email.trim().toLowerCase();
        String n = (name == null || name.isBlank()) ? "Demo User" : name.trim();
        return result(findOrCreate(e, n));
    }

    private AuthResult result(AppUser user) {
        String token = jwt.issue(user.getId(), user.getEmail(), user.getName(), DEMO_WORKSPACE,
            DEMO_WORKSPACE_NAME, user.isAdmin());
        return new AuthResult(token, user.getId(), user.getEmail(), user.getName(),
            DEMO_WORKSPACE, DEMO_WORKSPACE_NAME, user.isAdmin());
    }

    /** Ensure a platform admin account exists (idempotent) — runs at startup. */
    @Transactional
    public void ensureAdmin(String email, String password, String name) {
        String e = normalizeEmail(email);
        AppUser u = users.findByEmail(e).orElseGet(AppUser::new);
        boolean isNew = u.getId() == null;
        if (isNew) {
            u.setId(UUID.randomUUID());
            u.setOrgId(DEMO_ORG);
            u.setEmail(e);
            u.setCreatedAt(OffsetDateTime.now());
        }
        u.setName(name == null || name.isBlank() ? "Platform Admin" : name);
        u.setAdmin(true);
        if (password != null && !password.isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(password));
        }
        users.save(u);
        log.info("[auth] admin account ensured: {}", e);
    }

    private AppUser findOrCreate(String email, String name) {
        return users.findByEmail(email).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setId(UUID.randomUUID());
            u.setOrgId(DEMO_ORG);
            u.setEmail(email);
            u.setName(name);
            u.setCreatedAt(OffsetDateTime.now());
            return users.save(u);
        });
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone is required");
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.replaceAll("[^0-9]", "").length() < 7) {
            throw new IllegalArgumentException("Enter a valid phone number");
        }
        return digits;
    }

    private static String otpKey(String phone) {
        return "otp:" + phone;
    }

    public record AuthResult(String token, UUID userId, String email, String name,
                             UUID workspaceId, String workspaceName, boolean admin) {}
}
