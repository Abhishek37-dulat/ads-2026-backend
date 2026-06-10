package com.relay.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Fixed-window Redis rate limits for public authentication endpoints. */
@Service
public class AuthRateLimiter {

    private final StringRedisTemplate redis;

    public AuthRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String action, HttpServletRequest request, long limit, Duration window) {
        String key = "ratelimit:auth:" + action + ":" + clientIp(request);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            if (count != null && count > limit) {
                throw new RateLimitExceededException(
                    "Too many requests. Please try again later.");
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthService.DeliveryUnavailableException(
                "Authentication service is temporarily unavailable");
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
