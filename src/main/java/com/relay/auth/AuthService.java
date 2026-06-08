package com.relay.auth;

import com.relay.identity.AppUser;
import com.relay.identity.AppUserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Duration VERIFY_TTL = Duration.ofHours(24);

    private final AppUserRepository users;
    private final JwtService jwt;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;
    private final SmsService sms;
    private final String appBaseUrl;

    public AuthService(AppUserRepository users, JwtService jwt, StringRedisTemplate redis,
                       PasswordEncoder passwordEncoder, MailService mail, SmsService sms,
                       @Value("${relay.app.base-url:http://localhost:8081}") String appBaseUrl) {
        this.users = users;
        this.jwt = jwt;
        this.redis = redis;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.sms = sms;
        this.appBaseUrl = appBaseUrl;
    }

    /** Create an unverified password account and email a verification link. No session yet. */
    @Transactional
    public RegisterResult register(String email, String password, String name) {
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
        u.setEmailVerified(false);
        u.setCreatedAt(OffsetDateTime.now());
        users.save(u);
        return sendVerification(u);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        AppUser u = users.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));
        if (u.getPasswordHash() == null) {
            throw new IllegalArgumentException("This email uses Google or phone sign-in");
        }
        if (password == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect email or password");
        }
        if (!u.isEmailVerified()) {
            throw new EmailNotVerifiedException(u.getEmail());
        }
        return result(u);
    }

    /** Issue a fresh verification token and email it. */
    @Transactional
    public RegisterResult sendVerification(AppUser u) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(verifyKey(token), u.getEmail(), VERIFY_TTL);
        String link = appBaseUrl + "/api/auth/verify?token=" + token;
        boolean sent = mail.sendVerification(u.getEmail(), u.getName(), link);
        // In dev without SMTP, surface the link so the flow is still testable.
        return new RegisterResult(u.getEmail(), sent, sent ? null : link);
    }

    @Transactional
    public RegisterResult resendVerification(String email) {
        AppUser u = users.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));
        if (u.isEmailVerified()) {
            throw new IllegalArgumentException("This email is already verified — just sign in");
        }
        return sendVerification(u);
    }

    /** Consume a verification token → mark the user verified. Returns their email. */
    @Transactional
    public String verifyEmail(String token) {
        String key = verifyKey(token);
        String email = redis.opsForValue().get(key);
        if (email == null) {
            throw new IllegalArgumentException("This verification link is invalid or expired");
        }
        redis.delete(key);
        AppUser u = users.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Account no longer exists"));
        u.setEmailVerified(true);
        users.save(u);
        return email;
    }

    private static String normalizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email");
        }
        return email.trim().toLowerCase();
    }

    /**
     * Generate + store an OTP and text it via Fast2SMS. Returns the code only when SMS isn't
     * configured (dev fallback shown in the UI); returns null once a real SMS was dispatched.
     */
    public String startOtp(String phone) {
        String normalized = normalizePhone(phone);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        redis.opsForValue().set(otpKey(normalized), code, OTP_TTL);
        boolean sent = sms.sendOtp(normalized, code);
        if (!sent) {
            log.info("[otp] (no SMS) code for {} is {}", normalized, code); // dev only
            return code;
        }
        return null;
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
        u.setEmailVerified(true);
        if (password != null && !password.isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(password));
        }
        users.save(u);
        log.info("[auth] admin account ensured: {}", e);
    }

    /** Google/OTP identities are inherently verified (provider attests the identity). */
    private AppUser findOrCreate(String email, String name) {
        return users.findByEmail(email).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setId(UUID.randomUUID());
            u.setOrgId(DEMO_ORG);
            u.setEmail(email);
            u.setName(name);
            u.setEmailVerified(true);
            u.setCreatedAt(OffsetDateTime.now());
            return users.save(u);
        });
    }

    private static String verifyKey(String token) {
        return "verify:" + token;
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

    /** Outcome of registration — verification pending, no session issued yet. */
    public record RegisterResult(String email, boolean emailSent, String devVerifyUrl) {}

    /** Thrown when a verified-email-required account tries to sign in unverified. */
    public static class EmailNotVerifiedException extends RuntimeException {
        private final String email;
        public EmailNotVerifiedException(String email) {
            super("Please verify your email first — check your inbox.");
            this.email = email;
        }
        public String getEmail() { return email; }
    }
}
