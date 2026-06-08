package com.relay.auth;

import com.relay.auth.AuthService.AuthResult;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login flows: phone OTP + (demo) Google. All under /v1/auth/** which is public. */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Send an OTP to a phone. Demo: the code is returned as {@code devCode} (no real SMS). */
    @PostMapping("/otp/start")
    public Map<String, Object> startOtp(@RequestBody OtpStartRequest req) {
        String code = auth.startOtp(req.phone());
        return Map.of("sent", true, "devCode", code,
            "message", "Demo mode: use this code (a real build would text it).");
    }

    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody OtpVerifyRequest req) {
        return view(auth.verifyOtp(req.phone(), req.code()));
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        return view(auth.register(req.email(), req.password(), req.name()));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        return view(auth.login(req.email(), req.password()));
    }

    /** Demo Google sign-in — accepts an optional email/name, else a demo account. */
    @PostMapping("/google")
    public Map<String, Object> google(@RequestBody(required = false) GoogleRequest req) {
        return view(auth.googleLogin(req == null ? null : req.email(), req == null ? null : req.name()));
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

    public record GoogleRequest(String email, String name) {}
}
