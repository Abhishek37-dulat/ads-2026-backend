package com.relay.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthRateLimiterTest {

    @Test
    void rejectsRequestsPastTheLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(11L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        AuthRateLimiter limiter = new AuthRateLimiter(redis);

        assertThrows(AuthRateLimiter.RateLimitExceededException.class,
            () -> limiter.check("login", request, 10, Duration.ofMinutes(10)));
    }
}
