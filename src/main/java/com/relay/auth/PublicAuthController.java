package com.relay.auth;

import com.relay.auth.AuthService.AuthResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Browser-facing auth endpoints (full-page redirects), served at /api/auth:
 *  - Google OAuth 2.0 authorization-code flow (start → Google → callback)
 *  - email verification link landing
 *
 * The redirect URI is derived from the request host so the same build works across
 * d0187.in / www / admin / localhost, as long as each is registered in Google.
 */
@RestController
@RequestMapping("/api/auth")
public class PublicAuthController {

    private static final Logger log = LoggerFactory.getLogger(PublicAuthController.class);
    private static final String STATE_COOKIE = "g_oauth_state";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final AuthService auth;
    private final AuthRateLimiter rateLimiter;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUriOverride;
    private final RestClient http = RestClient.create();

    public PublicAuthController(AuthService auth, AuthRateLimiter rateLimiter,
                               @Value("${relay.google.client-id:}") String clientId,
                               @Value("${relay.google.client-secret:}") String clientSecret,
                               @Value("${relay.google.redirect-uri:}") String redirectUriOverride) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUriOverride = redirectUriOverride;
    }

    // -------------------------------------------------- Google: start
    @GetMapping("/google/start")
    public void googleStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        rateLimiter.check("google-start", request, 10, Duration.ofMinutes(10));
        if (!StringUtils.hasText(clientId)) {
            response.sendRedirect(base(request) + "/login?error=google_not_configured");
            return;
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        Cookie c = new Cookie(STATE_COOKIE, state);
        c.setHttpOnly(true);
        c.setPath("/api/auth");
        c.setMaxAge(600);
        c.setSecure(request.isSecure());
        response.addCookie(c);

        String url = AUTH_URI
            + "?client_id=" + enc(clientId)
            + "&redirect_uri=" + enc(redirectUri(request))
            + "&response_type=code"
            + "&scope=" + enc("openid email profile")
            + "&access_type=online"
            + "&prompt=select_account"
            + "&state=" + enc(state);
        response.sendRedirect(url);
    }

    // -------------------------------------------------- Google: callback
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        String base = base(request);
        try {
            if (StringUtils.hasText(error) || !StringUtils.hasText(code)) {
                throw new IllegalStateException("Google returned: " + error);
            }
            if (!stateMatches(request, state)) {
                throw new IllegalStateException("State mismatch");
            }

            // 1. exchange code → tokens
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("redirect_uri", redirectUri(request));
            form.add("grant_type", "authorization_code");
            @SuppressWarnings("unchecked")
            Map<String, Object> token = http.post().uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class);
            String accessToken = token == null ? null : (String) token.get("access_token");
            if (accessToken == null) {
                throw new IllegalStateException("No access_token from Google");
            }

            // 2. fetch profile
            @SuppressWarnings("unchecked")
            Map<String, Object> info = http.get().uri(USERINFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().body(Map.class);
            String email = info == null ? null : (String) info.get("email");
            String name = info == null ? null : (String) info.get("name");
            if (email == null) {
                throw new IllegalStateException("No email in Google profile");
            }

            // 3. find-or-create + issue our session
            AuthResult r = auth.googleLogin(email, name);
            clearState(response);
            String frag = "#token=" + enc(r.token())
                + "&email=" + enc(r.email())
                + "&name=" + enc(r.name())
                + "&workspace=" + enc(r.workspaceName());
            response.sendRedirect(base + "/auth/callback" + frag);
        } catch (Exception e) {
            log.warn("[google] callback failed: {}", e.getMessage());
            response.sendRedirect(base + "/login?error=google");
        }
    }

    // -------------------------------------------------- email verification landing
    @GetMapping("/verify")
    public void verify(@RequestParam String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String base = base(request);
        try {
            auth.verifyEmail(token);
            response.sendRedirect(base + "/login?verified=1");
        } catch (Exception e) {
            response.sendRedirect(base + "/login?verified=0");
        }
    }

    // -------------------------------------------------- helpers
    private String redirectUri(HttpServletRequest request) {
        return StringUtils.hasText(redirectUriOverride) ? redirectUriOverride : base(request) + "/api/auth/google/callback";
    }

    private static String base(HttpServletRequest request) {
        String proto = header(request, "X-Forwarded-Proto", request.getScheme());
        String host = header(request, "X-Forwarded-Host", request.getHeader("Host"));
        return proto + "://" + host;
    }

    private static String header(HttpServletRequest r, String name, String fallback) {
        String v = r.getHeader(name);
        return StringUtils.hasText(v) ? v.split(",")[0].trim() : fallback;
    }

    private static boolean stateMatches(HttpServletRequest request, String state) {
        if (!StringUtils.hasText(state) || request.getCookies() == null) return false;
        for (Cookie c : request.getCookies()) {
            if (STATE_COOKIE.equals(c.getName())) return state.equals(c.getValue());
        }
        return false;
    }

    private static void clearState(HttpServletResponse response) {
        Cookie c = new Cookie(STATE_COOKIE, "");
        c.setPath("/api/auth");
        c.setMaxAge(0);
        response.addCookie(c);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
