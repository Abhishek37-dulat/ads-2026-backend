package com.relay.auth;

import com.relay.auth.AuthService.AuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public login flows plus the authenticated current-session endpoint. */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService auth, AuthRateLimiter rateLimiter) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    /** Send an OTP to a phone via SMS (Fast2SMS). Falls back to a dev code if SMS isn't configured. */
    @PostMapping("/otp/start")
    public Map<String, Object> startOtp(@RequestBody OtpStartRequest req, HttpServletRequest request) {
        rateLimiter.check("otp-start", request, 5, Duration.ofMinutes(10));
        String devCode = auth.startOtp(req.phone());
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("sent", true);
        if (devCode != null) {
            out.put("devCode", devCode);
            out.put("channel", "demo");
            out.put("message", "SMS not configured — use this code.");
        } else {
            out.put("channel", "sms");
            out.put("message", "We texted you a 6-digit code.");
        }
        return out;
    }

    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody OtpVerifyRequest req, HttpServletRequest request) {
        rateLimiter.check("otp-verify", request, 10, Duration.ofMinutes(10));
        return view(auth.verifyOtp(req.phone(), req.code()));
    }

    /** Create a password account — no session; an email verification link is sent first. */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        rateLimiter.check("register", request, 5, Duration.ofHours(1));
        var r = auth.register(req.email(), req.password(), req.name());
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("pendingVerification", true);
        out.put("email", r.email());
        out.put("emailSent", r.emailSent());
        out.put("message", r.emailSent()
            ? "Check your inbox to verify your email, then sign in."
            : "Account created. Email isn't configured — use the verification link below.");
        if (r.devVerifyUrl() != null) {
            out.put("devVerifyUrl", r.devVerifyUrl());
        }
        return out;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        rateLimiter.check("login", request, 10, Duration.ofMinutes(10));
        return view(auth.login(req.email(), req.password()));
    }

    /** Resend the verification email for an unverified account. */
    @PostMapping("/verify/resend")
    public Map<String, Object> resend(@RequestBody ResendRequest req, HttpServletRequest request) {
        rateLimiter.check("verify-resend", request, 5, Duration.ofHours(1));
        var r = auth.resendVerification(req.email());
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("emailSent", r.emailSent());
        out.put("message", r.emailSent() ? "Verification email re-sent." : "Email not configured.");
        if (r.devVerifyUrl() != null) {
            out.put("devVerifyUrl", r.devVerifyUrl());
        }
        return out;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) {
            throw new IllegalStateException("Not authenticated");
        }
        return Map.of(
            "userId", p.userId().toString(),
            "email", p.email(),
            "name", p.name(),
            "workspaceId", p.workspaceId().toString(),
            "workspaceName", p.workspaceName(),
            "admin", p.admin());
    }

    private Map<String, Object> view(AuthResult r) {
        return Map.of(
            "token", r.token(),
            "user", Map.of("id", r.userId().toString(), "email", r.email(), "name", r.name()),
            "workspace", Map.of("id", r.workspaceId().toString(), "name", r.workspaceName()),
            "admin", r.admin());
    }

    public record RegisterRequest(@NotBlank String email, @NotBlank String password, String name) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record OtpStartRequest(@NotBlank String phone) {}

    public record OtpVerifyRequest(@NotBlank String phone, @NotBlank String code) {}

    public record ResendRequest(@NotBlank String email) {}
}
